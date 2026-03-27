from flask import Flask, request, jsonify
import akshare as ak
from datetime import datetime

app = Flask(__name__)


def normalize(code: str) -> str:
    c = (code or "").strip().upper()
    if not c:
        return "000001.SZ"
    if c.endswith(".SH") or c.endswith(".SZ"):
        return c
    if c.startswith("6"):
        return c + ".SH"
    return c + ".SZ"


def to_ak_code(code: str) -> str:
    # akshare stock_zh_a_spot_em uses plain 6-digit codes
    return code.split(".")[0]


@app.get("/health")
def health():
    return jsonify({"ok": True, "service": "akshare-bridge"})


@app.get("/quote")
def quote():
    code = normalize(request.args.get("code", ""))
    plain = to_ak_code(code)
    try:
        df = ak.stock_zh_a_spot_em()
        row = df[df["代码"] == plain]
        if row.empty:
            return jsonify({"ok": False, "code": code, "message": "symbol not found"}), 404
        r = row.iloc[0]
        current = float(r["最新价"])
        pre_close = float(r["昨收"])
        change = current - pre_close
        change_pct = 0 if pre_close == 0 else (change / pre_close) * 100
        return jsonify({
            "ok": True,
            "source": "akshare",
            "code": code,
            "name": str(r["名称"]),
            "open": float(r["今开"]),
            "preClose": pre_close,
            "current": current,
            "high": float(r["最高"]),
            "low": float(r["最低"]),
            "change": round(change, 2),
            "changePct": round(change_pct, 2),
            "volume": int(float(r["成交量"])),
            "amount": float(r["成交额"]),
            "updateTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "message": "Realtime quote from AKShare"
        })
    except Exception as ex:
        return jsonify({"ok": False, "code": code, "message": f"akshare error: {ex}"}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5005)
