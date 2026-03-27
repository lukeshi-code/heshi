package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StockQuoteService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, CacheEntry> quoteCache = new ConcurrentHashMap<String, CacheEntry>();
    private final ConcurrentHashMap<String, CacheEntry> seriesCache = new ConcurrentHashMap<String, CacheEntry>();
    private static final long CACHE_TTL_MS = 3000L;

    @Value("${app.tencent-quote-url:https://qt.gtimg.cn/q=}")
    private String tencentQuoteUrl;

    @Value("${app.tencent-minute-url:https://web.ifzq.gtimg.cn/appstock/app/minute/query?code=}")
    private String tencentMinuteUrl;

    @Value("${app.tencent-kline-url:https://web.ifzq.gtimg.cn/appstock/app/fqkline/get?param=}")
    private String tencentKlineUrl;

    @Value("${app.akshare-bridge-url:http://127.0.0.1:5005/quote?code=}")
    private String akshareBridgeUrl;

    public Map<String, Object> fetchQuote(String inputCode) {
        String normalized = normalizeInput(inputCode);
        CacheEntry cached = quoteCache.get(normalized);
        if (cached != null && !cached.expired()) {
            return cached.copy();
        }

        Map<String, Object> tencent = fetchFromTencent(normalized);
        if (tencent != null) {
            quoteCache.put(normalized, new CacheEntry(tencent));
            return tencent;
        }

        Map<String, Object> ak = fetchFromAkshareBridge(normalized);
        if (ak != null) {
            quoteCache.put(normalized, new CacheEntry(ak));
            return ak;
        }

        Map<String, Object> fallback = fallbackQuote(normalized, "Tencent and AKShare quote sources are unavailable");
        quoteCache.put(normalized, new CacheEntry(fallback));
        return fallback;
    }

    public Map<String, Object> fetchSeries(String inputCode, String inputPeriod) {
        String normalized = normalizeInput(inputCode);
        String period = normalizePeriod(inputPeriod);
        String cacheKey = normalized + "#" + period;
        CacheEntry cached = seriesCache.get(cacheKey);
        if (cached != null && !cached.expired()) {
            return cached.copy();
        }

        Map<String, Object> tencentSeries = fetchSeriesFromTencent(normalized, period);
        if (tencentSeries != null) {
            seriesCache.put(cacheKey, new CacheEntry(tencentSeries));
            return tencentSeries;
        }

        Map<String, Object> fallback = fallbackQuote(normalized, "Tencent series source is unavailable");
        Map<String, Object> out = fallbackSeries(normalized, period, fallback);
        seriesCache.put(cacheKey, new CacheEntry(out));
        return out;
    }

    public Map<String, Object> health() {
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("ok", true);
        out.put("quoteCacheSize", quoteCache.size());
        out.put("seriesCacheSize", seriesCache.size());
        Map<String, Object> ping = fetchFromTencent(normalizeCode("600519"));
        out.put("tencentOk", ping != null);
        return out;
    }

    private Map<String, Object> fetchFromTencent(String normalized) {
        try {
            String tencentCode = toTencentCode(normalized);
            String url = tencentQuoteUrl + URLEncoder.encode(tencentCode, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            try (InputStream in = conn.getInputStream()) {
                String body = readAsString(in, "GBK");
                return parseTencentQuote(normalized, body);
            }
        } catch (Exception ignore) {
            return null;
        }
    }

    private Map<String, Object> parseTencentQuote(String normalized, String body) {
        int start = body.indexOf("\"");
        int end = body.lastIndexOf("\"");
        if (start < 0 || end <= start) {
            return null;
        }
        String payload = body.substring(start + 1, end);
        String[] arr = payload.split("~");
        if (arr.length < 50) {
            return null;
        }

        String name = arr[1];
        double current = asDouble(arr[3]);
        double preClose = asDouble(arr[4]);
        double open = asDouble(arr[5]);
        long volume = (long) asDouble(arr[6]);
        double amountWan = asDouble(arr[37]);
        double high = asDouble(arr[33]);
        double low = asDouble(arr[34]);
        String dateTimeRaw = arr[30];

        if (current <= 0 && preClose <= 0) {
            return null;
        }
        if (preClose <= 0) {
            preClose = current;
        }
        if (open <= 0) {
            open = preClose;
        }
        if (high <= 0) {
            high = Math.max(open, current);
        }
        if (low <= 0) {
            low = Math.min(open, current);
        }

        double change = current - preClose;
        double changePct = preClose == 0 ? 0 : (change / preClose) * 100;

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("ok", true);
        map.put("source", "tencent");
        map.put("code", normalized);
        map.put("name", name);
        map.put("open", round(open));
        map.put("preClose", round(preClose));
        map.put("current", round(current));
        map.put("high", round(high));
        map.put("low", round(low));
        map.put("change", round(change));
        map.put("changePct", round(changePct));
        map.put("volume", volume);
        map.put("amount", round(amountWan * 10000));
        map.put("updateTime", formatTencentTime(dateTimeRaw));
        map.put("message", "Realtime quote");
        return map;
    }

    private Map<String, Object> fetchSeriesFromTencent(String normalized, String period) {
        return "min".equals(period) ? fetchMinuteSeries(normalized) : fetchKlineSeries(normalized, period);
    }

    private Map<String, Object> fetchMinuteSeries(String normalized) {
        try {
            String tencentCode = toTencentCode(normalized);
            String url = tencentMinuteUrl + URLEncoder.encode(tencentCode, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            try (InputStream in = conn.getInputStream()) {
                String body = readAsString(in, "UTF-8");
                Map<String, Object> root = objectMapper.readValue(body, Map.class);
                if (!isOkCode(root)) {
                    return null;
                }
                Map<String, Object> codeNode = getTencentDataNode(root, tencentCode);
                if (codeNode == null) {
                    return null;
                }
                Object dataNode = codeNode.get("data");
                if (!(dataNode instanceof Map)) {
                    return null;
                }
                Object linesObj = ((Map<?, ?>) dataNode).get("data");
                if (!(linesObj instanceof List)) {
                    return null;
                }
                List<?> lines = (List<?>) linesObj;
                List<Map<String, Object>> points = new ArrayList<Map<String, Object>>();
                double prevCumVol = 0d;
                for (Object lineObj : lines) {
                    if (lineObj == null) {
                        continue;
                    }
                    String[] cols = String.valueOf(lineObj).trim().split("\\s+");
                    if (cols.length < 4) {
                        continue;
                    }
                    String hm = formatHm(cols[0]);
                    double price = asDouble(cols[1]);
                    double cumVol = asDouble(cols[2]);
                    double cumAmount = asDouble(cols[3]);
                    double vol = Math.max(0d, cumVol - prevCumVol);
                    prevCumVol = cumVol;
                    double avg = cumVol > 0 ? (cumAmount / (cumVol * 100d)) : price;

                    Map<String, Object> point = new HashMap<String, Object>();
                    point.put("time", hm);
                    point.put("price", round(price));
                    point.put("avg", round(avg));
                    point.put("volume", (long) Math.round(vol * 100d));
                    points.add(point);
                }
                if (points.isEmpty()) {
                    return null;
                }
                return okSeries(normalized, "min", "tencent", points);
            }
        } catch (Exception ignore) {
            return null;
        }
    }

    private Map<String, Object> fetchKlineSeries(String normalized, String period) {
        try {
            String tencentCode = toTencentCode(normalized);
            String param = tencentCode + "," + period + ",,,120,";
            String url = tencentKlineUrl + URLEncoder.encode(param, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            try (InputStream in = conn.getInputStream()) {
                String body = readAsString(in, "UTF-8");
                Map<String, Object> root = objectMapper.readValue(body, Map.class);
                if (!isOkCode(root)) {
                    return null;
                }
                Map<String, Object> codeNode = getTencentDataNode(root, tencentCode);
                if (codeNode == null) {
                    return null;
                }
                Object rowsObj = codeNode.get(period);
                if (!(rowsObj instanceof List)) {
                    rowsObj = codeNode.get("qfq" + period);
                }
                if (!(rowsObj instanceof List)) {
                    return null;
                }
                List<?> rows = (List<?>) rowsObj;
                if (rows.isEmpty()) {
                    return null;
                }

                List<Map<String, Object>> points = new ArrayList<Map<String, Object>>();
                double sum = 0d;
                int n = 0;
                for (Object rowObj : rows) {
                    if (!(rowObj instanceof List)) {
                        continue;
                    }
                    List<?> row = (List<?>) rowObj;
                    if (row.size() < 6) {
                        continue;
                    }
                    String time = String.valueOf(row.get(0));
                    double open = asDouble(String.valueOf(row.get(1)));
                    double close = asDouble(String.valueOf(row.get(2)));
                    double high = asDouble(String.valueOf(row.get(3)));
                    double low = asDouble(String.valueOf(row.get(4)));
                    double vol = asDouble(String.valueOf(row.get(5)));
                    sum += close;
                    n++;
                    double avg = n == 0 ? close : (sum / n);

                    Map<String, Object> point = new HashMap<String, Object>();
                    point.put("time", time);
                    point.put("price", round(close));
                    point.put("open", round(open > 0 ? open : close));
                    point.put("high", round(high > 0 ? high : Math.max(open, close)));
                    point.put("low", round(low > 0 ? low : Math.min(open, close)));
                    point.put("close", round(close));
                    point.put("avg", round(avg));
                    point.put("volume", (long) Math.round(vol * 100d));
                    points.add(point);
                }
                if (points.isEmpty()) {
                    return null;
                }
                return okSeries(normalized, period, "tencent", points);
            }
        } catch (Exception ignore) {
            return null;
        }
    }

    private Map<String, Object> fetchFromAkshareBridge(String normalized) {
        try {
            String url = akshareBridgeUrl + URLEncoder.encode(normalized, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int status = conn.getResponseCode();
            if (status / 100 != 2) {
                return null;
            }
            try (InputStream in = conn.getInputStream()) {
                String body = readAsString(in, "UTF-8");
                Map<String, Object> map = objectMapper.readValue(body, Map.class);
                Object ok = map.get("ok");
                if (ok instanceof Boolean && (Boolean) ok) {
                    return map;
                }
            }
        } catch (Exception ignore) {
            return null;
        }
        return null;
    }

    private Map<String, Object> fallbackQuote(String normalized, String message) {
        double base = 10 + Math.abs(normalized.hashCode() % 2000) / 10.0;
        double open = round(base * 0.995);
        double current = round(base * 1.003);
        double preClose = round(base);
        double high = round(Math.max(open, current) * 1.01);
        double low = round(Math.min(open, current) * 0.99);
        double change = round(current - preClose);
        double changePct = preClose == 0 ? 0 : round(change / preClose * 100);
        long volume = Math.abs(normalized.hashCode() % 9000000) + 100000;
        double amount = round(volume * current);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("ok", false);
        map.put("source", "offline");
        map.put("code", normalized);
        map.put("name", normalized);
        map.put("open", open);
        map.put("preClose", preClose);
        map.put("current", current);
        map.put("high", high);
        map.put("low", low);
        map.put("change", change);
        map.put("changePct", changePct);
        map.put("volume", volume);
        map.put("amount", amount);
        map.put("updateTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        map.put("message", message + " (offline simulated quote shown)");
        return map;
    }

    private Map<String, Object> fallbackSeries(String normalized, String period, Map<String, Object> quote) {
        double base = asDouble(String.valueOf(quote.get("current")));
        if (base <= 0) {
            base = 100d;
        }
        int pointsCount = "min".equals(period) ? 120 : ("day".equals(period) ? 80 : ("week".equals(period) ? 60 : 48));
        double drift = base * ("min".equals(period) ? 0.004 : ("day".equals(period) ? 0.008 : 0.012));
        long volBase = "min".equals(period) ? 9000 : ("day".equals(period) ? 16000 : 26000);

        List<Map<String, Object>> points = new ArrayList<Map<String, Object>>();
        double p = base;
        double sum = 0d;
        for (int i = 0; i < pointsCount; i++) {
            double rnd = (Math.random() - 0.45) * drift;
            p = Math.max(0.01, p + rnd);
            sum += p;
            Map<String, Object> point = new HashMap<String, Object>();
            point.put("time", "min".equals(period) ? String.format("%04d", 930 + i) : ("P" + (i + 1)));
            point.put("price", round(p));
            if (!"min".equals(period)) {
                point.put("open", round(p * 0.997));
                point.put("high", round(p * 1.006));
                point.put("low", round(p * 0.994));
                point.put("close", round(p));
            }
            point.put("avg", round(sum / (i + 1)));
            point.put("volume", (long) (volBase * (0.2 + Math.random())));
            points.add(point);
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("ok", false);
        map.put("source", "offline");
        map.put("code", normalized);
        map.put("period", period);
        map.put("points", points);
        map.put("message", "Offline simulated series");
        return map;
    }

    private Map<String, Object> okSeries(String normalized, String period, String source, List<Map<String, Object>> points) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("ok", true);
        map.put("source", source);
        map.put("code", normalized);
        map.put("period", period);
        map.put("points", points);
        map.put("message", "Realtime series");
        return map;
    }

    private Map<String, Object> getTencentDataNode(Map<String, Object> root, String tencentCode) {
        Object dataObj = root.get("data");
        if (!(dataObj instanceof Map)) {
            return null;
        }
        Object codeObj = ((Map<?, ?>) dataObj).get(tencentCode);
        if (!(codeObj instanceof Map)) {
            return null;
        }
        return (Map<String, Object>) codeObj;
    }

    private boolean isOkCode(Map<String, Object> root) {
        Object codeObj = root.get("code");
        if (codeObj instanceof Number) {
            return ((Number) codeObj).intValue() == 0;
        }
        return "0".equals(String.valueOf(codeObj));
    }

    private String normalizeInput(String raw) {
        String s = raw == null ? "" : raw.trim().toUpperCase();
        if (s.isEmpty()) {
            return "000001.SZ";
        }
        if (isCodeInput(s)) {
            return normalizeCode(s);
        }
        String byTencent = resolveCodeByTencentSuggest(raw == null ? "" : raw.trim());
        if (byTencent != null) {
            return byTencent;
        }
        String bySina = resolveCodeBySinaSuggest(raw == null ? "" : raw.trim());
        if (bySina != null) {
            return bySina;
        }
        return normalizeCode(s);
    }

    private boolean isCodeInput(String s) {
        return s.matches("^\\d{6}([.](SH|SZ|BJ))?$");
    }

    private String normalizeCode(String s) {
        String t = s.trim().toUpperCase();
        if (t.endsWith(".SH") || t.endsWith(".SZ") || t.endsWith(".BJ")) {
            return t;
        }
        if (t.startsWith("6")) {
            return t + ".SH";
        }
        if (t.startsWith("8") || t.startsWith("4")) {
            return t + ".BJ";
        }
        return t + ".SZ";
    }

    private String resolveCodeByTencentSuggest(String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String url = "https://smartbox.gtimg.cn/s3/?q=" + URLEncoder.encode(keyword.trim(), "UTF-8") + "&t=all";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            try (InputStream in = conn.getInputStream()) {
                String body = readAsString(in, "UTF-8");
                int q1 = body.indexOf("\"");
                int q2 = body.lastIndexOf("\"");
                if (q1 < 0 || q2 <= q1) {
                    return null;
                }
                String payload = decodeUnicodeEscapes(body.substring(q1 + 1, q2));
                if (payload.trim().isEmpty()) {
                    return null;
                }
                String first = payload.split("\\^")[0];
                String[] cols = first.split("~");
                if (cols.length < 2) {
                    return null;
                }
                String market = cols[0].toLowerCase();
                String code = cols[1].trim();
                if (!code.matches("^\\d{6}$")) {
                    return null;
                }
                if ("sh".equals(market)) {
                    return code + ".SH";
                }
                if ("sz".equals(market)) {
                    return code + ".SZ";
                }
                if ("bj".equals(market)) {
                    return code + ".BJ";
                }
                return null;
            }
        } catch (Exception ignore) {
            return null;
        }
    }

    private String resolveCodeBySinaSuggest(String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String url = "https://suggest3.sinajs.cn/suggest/type=11,12,13,14,15&key=" + URLEncoder.encode(keyword.trim(), "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            try (InputStream in = conn.getInputStream()) {
                String body = readAsString(in, "UTF-8");
                int q1 = body.indexOf("\"");
                int q2 = body.lastIndexOf("\"");
                if (q1 < 0 || q2 <= q1) {
                    return null;
                }
                String payload = body.substring(q1 + 1, q2);
                if (payload.trim().isEmpty()) {
                    return null;
                }
                String first = payload.split(";")[0];
                String[] cols = first.split(",");
                if (cols.length < 4) {
                    return null;
                }
                String symbol = cols[3].trim().toLowerCase();
                if (symbol.matches("^(sh|sz|bj)\\d{6}$")) {
                    String market = symbol.substring(0, 2).toUpperCase();
                    String code = symbol.substring(2);
                    return code + "." + market;
                }
                return null;
            }
        } catch (Exception ignore) {
            return null;
        }
    }

    private String decodeUnicodeEscapes(String s) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 5 < s.length() && s.charAt(i + 1) == 'u') {
                String hex = s.substring(i + 2, i + 6);
                try {
                    out.append((char) Integer.parseInt(hex, 16));
                    i += 6;
                    continue;
                } catch (Exception ignore) {
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private String normalizePeriod(String period) {
        String p = period == null ? "min" : period.trim().toLowerCase();
        if ("day".equals(p) || "week".equals(p) || "month".equals(p) || "min".equals(p)) {
            return p;
        }
        return "min";
    }

    private String toTencentCode(String normalized) {
        if (normalized.endsWith(".SH")) {
            return "sh" + normalized.substring(0, normalized.length() - 3);
        }
        return "sz" + normalized.substring(0, normalized.length() - 3);
    }

    private String formatTencentTime(String t) {
        String s = t == null ? "" : t.trim();
        if (s.length() == 14) {
            return s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6, 8) + " "
                + s.substring(8, 10) + ":" + s.substring(10, 12) + ":" + s.substring(12, 14);
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private String formatHm(String hhmm) {
        if (hhmm == null) {
            return "";
        }
        String s = hhmm.trim();
        if (s.length() == 4) {
            return s.substring(0, 2) + ":" + s.substring(2, 4);
        }
        return s;
    }

    private String readAsString(InputStream in, String encoding) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
        return new String(out.toByteArray(), encoding);
    }

    private double asDouble(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception ex) {
            return 0d;
        }
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static class CacheEntry {
        private final Map<String, Object> data;
        private final long ts;

        private CacheEntry(Map<String, Object> data) {
            this.data = new HashMap<String, Object>(data);
            this.ts = System.currentTimeMillis();
        }

        private boolean expired() {
            return System.currentTimeMillis() - ts > CACHE_TTL_MS;
        }

        private Map<String, Object> copy() {
            return new HashMap<String, Object>(data);
        }
    }
}
