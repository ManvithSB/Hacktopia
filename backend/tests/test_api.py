import unittest
from fastapi.testclient import TestClient
from backend.main import app

class TestAPI(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_health_check(self):
        response = self.client.get("/health")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"status": "ok"})

    def test_valid_analyze_request(self):
        payload = {
            "message": "Your electricity bill is overdue. Pay rs 850 immediately.",
            "url": "https://example.com",
            "qr_payload": "some_qr_data",
            "payee": "ABC Utilities",
            "amount": 850.0,
            "currency": "INR"
        }
        response = self.client.post("/analyze", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertIn("interaction_id", data)
        self.assertEqual(data["risk_level"], "SAFE")
        self.assertGreaterEqual(data["score"], 0)
        self.assertEqual(data["coverage"]["message"], "checked")
        self.assertEqual(data["coverage"]["url"], "checked")
        self.assertEqual(data["coverage"]["qr"], "checked")
        self.assertEqual(data["coverage"]["payment"], "checked")

    def test_message_only_request(self):
        payload = {
            "message": "Hello friend, I need money."
        }
        response = self.client.post("/analyze", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["coverage"]["message"], "checked")
        self.assertEqual(data["coverage"]["url"], "not_provided")
        self.assertEqual(data["coverage"]["qr"], "not_provided")
        self.assertEqual(data["coverage"]["payment"], "not_provided")

    def test_message_and_payment_request(self):
        payload = {
            "message": "Please pay for your order.",
            "payee": "Fake Store",
            "amount": 500
        }
        response = self.client.post("/analyze", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["coverage"]["message"], "checked")
        self.assertEqual(data["coverage"]["payment"], "checked")

    def test_empty_request_rejection(self):
        payload = {}
        response = self.client.post("/analyze", json=payload)
        self.assertEqual(response.status_code, 422)  # Unprocessable Entity
        
        # Or testing with all fields explicitly set to None/empty
        payload = {
            "message": None,
            "url": None,
            "qr_payload": None,
            "payee": None,
            "amount": None
        }
        response = self.client.post("/analyze", json=payload)
        self.assertEqual(response.status_code, 422)

    def test_negative_amount_rejection(self):
        payload = {
            "message": "Test",
            "amount": -100.5
        }
        response = self.client.post("/analyze", json=payload)
        self.assertEqual(response.status_code, 422)
        data = response.json()
        self.assertIn("detail", data)

if __name__ == "__main__":
    unittest.main()
