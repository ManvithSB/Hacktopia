import unittest
from backend.engine.explanations import get_reason
from fastapi.testclient import TestClient
from backend.main import app

class TestExplanations(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_english_explanation(self):
        reason = get_reason("urgent_language", "en")
        self.assertEqual(reason, "The message creates urgency and pressures you to act quickly.")

    def test_hindi_explanation(self):
        reason = get_reason("urgent_language", "hi")
        self.assertEqual(reason, "यह संदेश जल्दी कार्रवाई करने का दबाव बनाता है।")

    def test_kannada_explanation(self):
        reason = get_reason("urgent_language", "kn")
        self.assertEqual(reason, "ಈ ಸಂದೇಶವು ತ್ವರಿತವಾಗಿ ಕ್ರಮ ಕೈಗೊಳ್ಳುವಂತೆ ಒತ್ತಡ ಹೇರುತ್ತದೆ.")

    def test_all_12_signals_have_translations(self):
        from backend.engine.explanations import TRANSLATIONS
        signals = [
            "urgent_language", "account_threat", "kyc_pressure", "impersonation", 
            "credential_request", "payment_pressure", "suspicious_intent", 
            "lottery_prize_scam", "refund_reward_scam", "amount_mismatch", 
            "payee_mismatch", "payment_context_mismatch"
        ]
        for sig in signals:
            self.assertIn(sig, TRANSLATIONS)
            self.assertIn("en", TRANSLATIONS[sig])
            self.assertIn("hi", TRANSLATIONS[sig])
            self.assertIn("kn", TRANSLATIONS[sig])

    def test_unsupported_language_fallback(self):
        reason = get_reason("urgent_language", "fr")
        self.assertEqual(reason, "The message creates urgency and pressures you to act quickly.")

    def test_uppercase_language_code(self):
        reason = get_reason("urgent_language", "HI")
        self.assertEqual(reason, "यह संदेश जल्दी कार्रवाई करने का दबाव बनाता है।")

    def test_api_english(self):
        payload = {
            "message": "URGENT! Complete KYC immediately.",
            "language": "en"
        }
        response = self.client.post("/analyze", json=payload)
        data = response.json()
        reasons = data["reasons"]
        self.assertIn("The message creates urgency and pressures you to act quickly.", reasons)

    def test_api_hindi(self):
        payload = {
            "message": "तुरंत करें! KYC पूरा करें।",
            "language": "hi"
        }
        response = self.client.post("/analyze", json=payload)
        data = response.json()
        reasons = data["reasons"]
        self.assertIn("यह संदेश जल्दी कार्रवाई करने का दबाव बनाता है।", reasons)

    def test_api_kannada(self):
        payload = {
            "message": "ತಕ್ಷಣ! KYC ಪೂರ್ಣಗೊಳಿಸಿ.",
            "language": "kn"
        }
        response = self.client.post("/analyze", json=payload)
        data = response.json()
        reasons = data["reasons"]
        self.assertIn("ಈ ಸಂದೇಶವು ತ್ವರಿತವಾಗಿ ಕ್ರಮ ಕೈಗೊಳ್ಳುವಂತೆ ಒತ್ತಡ ಹೇರುತ್ತದೆ.", reasons)

if __name__ == "__main__":
    unittest.main()
