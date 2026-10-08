import unittest
from backend.engine.message_shield import analyze_message

class TestMessageShield(unittest.TestCase):
    def test_safe_message(self):
        msg = "Your order has been delivered successfully."
        result = analyze_message(msg)
        self.assertEqual(result["risk_level"], "SAFE")
        self.assertEqual(result["recommendation"], "PROCEED")
        self.assertEqual(result["score"], 0)
        self.assertEqual(len(result["signals"]), 0)

    def test_english_kyc_scam(self):
        msg = "URGENT! Your bank account will be blocked today. Complete KYC immediately."
        result = analyze_message(msg)
        self.assertIn(result["risk_level"], ["SUSPICIOUS", "HIGH_RISK"])
        
        signals = result["signals"]
        self.assertIn("urgent_language", signals)
        self.assertIn("account_threat", signals)
        self.assertIn("kyc_pressure", signals)
        
    def test_credential_scam(self):
        msg = "Your account will be suspended. Send your OTP and UPI PIN to verify."
        result = analyze_message(msg)
        self.assertEqual(result["risk_level"], "HIGH_RISK")
        self.assertEqual(result["recommendation"], "STOP")
        signals = result["signals"]
        self.assertIn("account_threat", signals)
        self.assertIn("credential_request", signals)

    def test_lottery_scam(self):
        msg = "Congratulations! You won a ₹50,000 lottery. Pay ₹500 processing fee to claim your prize."
        result = analyze_message(msg)
        self.assertEqual(result["risk_level"], "HIGH_RISK")
        signals = result["signals"]
        self.assertIn("lottery_prize_scam", signals)
        # We also look for suspicious_intent like "pay processing fee"
        self.assertTrue("suspicious_intent" in signals or "payment_pressure" in signals)

    def test_hindi_scam_message(self):
        msg = "तुरंत करें! आपका बैंक खाता बंद हो जाएगा। OTP भेजें।"
        result = analyze_message(msg)
        signals = result["signals"]
        self.assertIn("urgent_language", signals)
        self.assertIn("account_threat", signals)
        self.assertIn("credential_request", signals)
        self.assertEqual(result["risk_level"], "HIGH_RISK")

    def test_kannada_scam_message(self):
        msg = "ತಕ್ಷಣ! ನಿಮ್ಮ ಖಾತೆ ಬ್ಲಾಕ್ ಆಗಲಿದೆ. ನಿಮ್ಮ OTP ಕಳುಹಿಸಿ."
        result = analyze_message(msg)
        signals = result["signals"]
        self.assertIn("urgent_language", signals)
        self.assertIn("account_threat", signals)
        self.assertIn("credential_request", signals)
        self.assertEqual(result["risk_level"], "HIGH_RISK")

    def test_duplicate_keywords_do_not_inflate_score(self):
        # Even with multiple urgent words, it should only count the signal once
        msg = "urgent! act now immediately today!"
        result = analyze_message(msg)
        self.assertEqual(result["score"], 15)  # only +15 for urgent_language
        self.assertEqual(len(result["signals"]), 1)
        self.assertEqual(result["signals"][0], "urgent_language")

if __name__ == "__main__":
    unittest.main()
