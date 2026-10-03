import sys
sys.path.insert(0, "backend")
import unittest

from dashboard_contract import _dashboard_chain, _dashboard_symbol


class DashboardContractTests(unittest.TestCase):
    def test_symbol_aliases_are_normalized(self):
        self.assertEqual(_dashboard_symbol("NIFTY 50"), "NIFTY")
        self.assertEqual(_dashboard_symbol("NIFTYBANK"), "BANKNIFTY")
        self.assertEqual(_dashboard_symbol("MIDCAP SELECT"), "MIDCPNIFTY")

    def test_dashboard_chain_preserves_option_values(self):
        payload = {
            "rows": [{
                "strike": 25000,
                "ce": {"oi": 100, "chg_oi": 10, "ltp": 120},
                "pe": {"oi": 200, "chg_oi": -5, "ltp": 80},
            }]
        }
        self.assertEqual(_dashboard_chain(payload), [{
            "strike": 25000,
            "ce": {"oi": 100, "oi_chg": 10, "ltp": 120},
            "pe": {"oi": 200, "oi_chg": -5, "ltp": 80},
        }])


if __name__ == "__main__":
    unittest.main()
