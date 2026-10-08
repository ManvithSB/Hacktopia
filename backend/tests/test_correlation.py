import unittest
from backend.engine.correlation import correlate_context

class TestCorrelation(unittest.TestCase):
    def test_matching_amount(self):
        msg = "Your electricity bill is ₹850. Pay to ABC Electricity."
        result = correlate_context(msg, "ABC Electricity", 850)
        signals = result["correlation_signals"]
        self.assertNotIn("amount_mismatch", signals)
        self.assertNotIn("payee_mismatch", signals)

    def test_amount_mismatch(self):
        msg = "Your electricity bill is ₹850. Pay to ABC Electricity."
        result = correlate_context(msg, "ABC Electricity", 8500)
        signals = result["correlation_signals"]
        self.assertIn("amount_mismatch", signals)
        self.assertNotIn("payee_mismatch", signals)

    def test_payee_mismatch(self):
        msg = "Your electricity bill is ₹850. Pay to ABC Electricity."
        result = correlate_context(msg, "Rahul Kumar", 850)
        signals = result["correlation_signals"]
        self.assertNotIn("amount_mismatch", signals)
        self.assertIn("payee_mismatch", signals)

    def test_both_mismatch(self):
        msg = "Your electricity bill is ₹850. Pay to ABC Electricity."
        result = correlate_context(msg, "Rahul Kumar", 8500)
        signals = result["correlation_signals"]
        self.assertIn("amount_mismatch", signals)
        self.assertIn("payee_mismatch", signals)
        self.assertIn("payment_context_mismatch", signals)
        self.assertGreater(result["correlation_score"], 40) # 20+25+10 = 55

    def test_missing_message_amount(self):
        msg = "Please pay your electricity bill immediately."
        result = correlate_context(msg, "ABC Electricity", 850)
        signals = result["correlation_signals"]
        self.assertNotIn("amount_mismatch", signals)

    def test_missing_payment_amount(self):
        msg = "Pay ₹850 to ABC Electricity."
        result = correlate_context(msg, "ABC Electricity", None)
        signals = result["correlation_signals"]
        self.assertNotIn("amount_mismatch", signals)

    def test_missing_message_payee(self):
        msg = "Please pay your electricity bill of ₹850."
        result = correlate_context(msg, "ABC Electricity", 850)
        signals = result["correlation_signals"]
        self.assertNotIn("payee_mismatch", signals)

    def test_payee_normalization(self):
        msg = "Pay ABC Utilities ₹850."
        result = correlate_context(msg, " abc utilities ", 850)
        signals = result["correlation_signals"]
        self.assertNotIn("payee_mismatch", signals)

    def test_inr_formatting(self):
        msg = "Pay ₹1,000 to ABC Utilities."
        result = correlate_context(msg, "ABC Utilities", 1000)
        signals = result["correlation_signals"]
        self.assertNotIn("amount_mismatch", signals)

if __name__ == "__main__":
    unittest.main()
