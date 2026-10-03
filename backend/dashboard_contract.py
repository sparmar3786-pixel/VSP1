"""Pure dashboard contract helpers; no FastAPI or network imports."""

NSE_DASH_INDEXES = (
    {"symbol": "NIFTY", "name": "NIFTY 50", "exchange": "NSE"},
    {"symbol": "BANKNIFTY", "name": "NIFTY Bank", "exchange": "NSE"},
    {"symbol": "FINNIFTY", "name": "NIFTY Financial Services", "exchange": "NSE"},
    {"symbol": "MIDCPNIFTY", "name": "NIFTY Midcap Select", "exchange": "NSE"},
    {"symbol": "NIFTYNEXT50", "name": "NIFTY Next 50", "exchange": "NSE"},
    {"symbol": "NIFTYFPI150", "name": "NIFTY India FPI 150", "exchange": "NSE"},
    {"symbol": "SENSEX", "name": "SENSEX", "exchange": "BSE"},
    {"symbol": "BANKEX", "name": "BANKEX", "exchange": "BSE"},
)
MCX_DASH_INDEXES = (
    {"symbol": "MCXBULLDEX", "name": "MCX BULLDEX", "exchange": "MCX"},
    {"symbol": "MCXMETLDEX", "name": "MCX METLDEX", "exchange": "MCX"},
    {"symbol": "MCXENRGDEX", "name": "MCX ENRGDEX", "exchange": "MCX"},
)


def _dashboard_symbol(symbol: str) -> str:
    s = str(symbol or "NIFTY").upper().replace(" ", "").replace("-", "")
    aliases = {
        "NIFTY50": "NIFTY",
        "NIFTYBANK": "BANKNIFTY",
        "BANKNIFTY": "BANKNIFTY",
        "MIDCAPSELECT": "MIDCPNIFTY",
        "NIFTYFINANCIALSERVICES": "FINNIFTY",
    }
    return aliases.get(s, s)


def _dashboard_chain(payload):
    rows = []
    for r in payload.get("rows", []) or []:
        ce = r.get("ce", {})
        pe = r.get("pe", {})
        rows.append({
            "strike": r.get("strike"),
            "ce": {"oi": ce.get("oi", 0), "oi_chg": ce.get("chg_oi", 0), "ltp": ce.get("ltp", 0)},
            "pe": {"oi": pe.get("oi", 0), "oi_chg": pe.get("chg_oi", 0), "ltp": pe.get("ltp", 0)},
        })
    return rows
