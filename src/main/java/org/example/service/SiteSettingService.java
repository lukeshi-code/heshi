package org.example.service;

import org.example.model.SiteSetting;
import org.example.repository.SiteSettingRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SiteSettingService {
    private final SiteSettingRepository siteSettingRepository;

    public SiteSettingService(SiteSettingRepository siteSettingRepository) {
        this.siteSettingRepository = siteSettingRepository;
    }

    public void ensureDefaults() {
        ensure("theme.primaryColor", "#0f172a", "后台主色");
        ensure("theme.accentColor", "#1d4ed8", "后台强调色");
        ensure("theme.logoText", "网站后台管理", "后台 Logo 文案");
        ensure("theme.bannerUrl", "", "后台默认 Banner 图 URL");
        ensure("system.siteTitle", "和石资本", "站点名称");
        ensure("system.footerText", "© 和石资本", "页脚文案");
        ensure("system.maintenanceMode", "false", "维护模式");
        ensure("system.allowRegister", "true", "是否允许注册");
        ensure("system.stockDataProvider", "AKShare/Tencent", "行情数据源说明");
    }

    public Map<String, String> map() {
        List<SiteSetting> rows = siteSettingRepository.findAll();
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (SiteSetting row : rows) {
            out.put(row.getSettingKey(), row.getSettingValue());
        }
        return out;
    }

    public void save(String key, String value, String description) {
        if (key == null || key.trim().isEmpty()) return;
        SiteSetting row = siteSettingRepository.findById(key.trim()).orElseGet(SiteSetting::new);
        row.setSettingKey(key.trim());
        row.setSettingValue(value == null ? "" : value.trim());
        if (description != null) row.setDescription(description);
        siteSettingRepository.save(row);
    }

    private void ensure(String key, String value, String description) {
        if (siteSettingRepository.existsById(key)) return;
        SiteSetting row = new SiteSetting();
        row.setSettingKey(key);
        row.setSettingValue(value);
        row.setDescription(description);
        siteSettingRepository.save(row);
    }
}
