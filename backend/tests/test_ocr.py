"""Tests for the real OCR pipeline (pytesseract + Tesseract).

These tests generate images programmatically with rendered text and verify
that actual pixel-based OCR extracts the text and feeds it into the
Message Shield pipeline.

NOTE: These tests REQUIRE the Tesseract binary to be installed on the system.
      If Tesseract is not installed, tests will be skipped (not faked).
"""

import io
import os
import unittest

from PIL import Image, ImageDraw, ImageFont

# ---------------------------------------------------------------------------
# Check whether Tesseract is available before importing test client
# ---------------------------------------------------------------------------
_tesseract_available = True
_skip_reason = ""
try:
    import pytesseract
    if os.name == 'nt':
        common_paths = [
            r'C:\Program Files\Tesseract-OCR\tesseract.exe',
            r'C:\Program Files (x86)\Tesseract-OCR\tesseract.exe',
            r'C:\Users\JATHAN\AppData\Local\Tesseract-OCR\tesseract.exe'
        ]
        for path in common_paths:
            if os.path.exists(path):
                pytesseract.pytesseract.tesseract_cmd = path
                break
    pytesseract.get_tesseract_version()
except Exception as exc:
    _tesseract_available = False
    _skip_reason = f"Tesseract not available: {exc}"

from fastapi.testclient import TestClient
from backend.main import app

client = TestClient(app)


# ---------------------------------------------------------------------------
# Helpers: Generate test images with real rendered text
# ---------------------------------------------------------------------------
def _get_font(size: int = 40):
    """Return a TrueType font. Falls back to Pillow default if none found."""
    # Try common Windows fonts first
    for name in ["arial.ttf", "verdana.ttf", "calibri.ttf", "consola.ttf"]:
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    # Try common Linux paths
    for path in [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/TTF/DejaVuSans.ttf",
    ]:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                continue
    # Absolute fallback – Pillow built-in bitmap font
    return ImageFont.load_default()


def create_image_with_text(text: str, fmt: str = "PNG", size=(1200, 200)) -> bytes:
    """Create an image with the given text rendered as pixels.

    Args:
        text: The text to render onto the image.
        fmt: Image format – "PNG", "JPEG", or "WEBP".
        size: (width, height) tuple.

    Returns:
        Raw image bytes in the requested format.
    """
    img = Image.new("RGB", size, color="white")
    draw = ImageDraw.Draw(img)
    font = _get_font(36)
    draw.text((20, 40), text, fill="black", font=font)

    buf = io.BytesIO()
    save_kwargs = {}
    if fmt == "JPEG":
        # JPEG doesn't support RGBA
        save_kwargs["quality"] = 95
    img.save(buf, format=fmt, **save_kwargs)
    return buf.getvalue()


def create_blank_image(fmt: str = "PNG", size=(200, 200)) -> bytes:
    """Create a blank white image with no text."""
    img = Image.new("RGB", size, color="white")
    buf = io.BytesIO()
    img.save(buf, format=fmt)
    return buf.getvalue()


# ---------------------------------------------------------------------------
# Test cases
# ---------------------------------------------------------------------------
@unittest.skipUnless(_tesseract_available, _skip_reason)
class TestOCRPipeline(unittest.TestCase):
    """Tests that require a working Tesseract installation."""

    # ------------------------------------------------------------------
    # 1. Real PNG with scam text → OCR → Message Shield signals
    # ------------------------------------------------------------------
    def test_png_scam_text(self):
        text = "URGENT your bank account will be blocked complete KYC now"
        img_bytes = create_image_with_text(text, fmt="PNG")
        resp = client.post(
            "/analyze/image",
            files={"image": ("scam.png", img_bytes, "image/png")},
            data={"language": "en"},
        )
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        # The extracted text should contain key scam phrases
        extracted = data["extracted_text"].lower()
        self.assertIn("urgent", extracted)
        self.assertIn("bank", extracted)
        # Message Shield must have detected signals
        self.assertTrue(len(data["signals"]) > 0, "Expected at least one signal")
        self.assertEqual(data["input_source"], "image")
        self.assertIn("reasons", data)
        self.assertIn("risk_level", data)

    # ------------------------------------------------------------------
    # 2. Real JPEG with scam text
    # ------------------------------------------------------------------
    def test_jpeg_scam_text(self):
        text = "You won a lottery prize claim now send payment"
        img_bytes = create_image_with_text(text, fmt="JPEG")
        resp = client.post(
            "/analyze/image",
            files={"image": ("scam.jpg", img_bytes, "image/jpeg")},
            data={"language": "en"},
        )
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        extracted = data["extracted_text"].lower()
        self.assertIn("lottery", extracted)
        self.assertTrue(len(data["signals"]) > 0, "Expected at least one signal")
        self.assertEqual(data["input_source"], "image")

    # ------------------------------------------------------------------
    # 3. Real WEBP with scam text
    # ------------------------------------------------------------------
    def test_webp_scam_text(self):
        text = "URGENT send money immediately or account blocked"
        img_bytes = create_image_with_text(text, fmt="WEBP")
        resp = client.post(
            "/analyze/image",
            files={"image": ("scam.webp", img_bytes, "image/webp")},
            data={"language": "en"},
        )
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        extracted = data["extracted_text"].lower()
        self.assertIn("urgent", extracted)
        self.assertEqual(data["input_source"], "image")

    # ------------------------------------------------------------------
    # 4. OCR output reaches Message Shield and produces reasons
    # ------------------------------------------------------------------
    def test_ocr_feeds_message_shield(self):
        text = "Dear customer your account is suspended verify KYC immediately"
        img_bytes = create_image_with_text(text, fmt="PNG")
        resp = client.post(
            "/analyze/image",
            files={"image": ("kyc.png", img_bytes, "image/png")},
            data={"language": "en"},
        )
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        # Must have signals AND corresponding reasons
        self.assertTrue(len(data["signals"]) > 0)
        self.assertEqual(len(data["reasons"]), len(data["signals"]))
        # Coverage should indicate OCR path
        self.assertEqual(data["coverage"]["message"], "checked_via_ocr")

    # ------------------------------------------------------------------
    # 5. Multilingual explanations on OCR output
    # ------------------------------------------------------------------
    def test_ocr_multilingual_hindi(self):
        text = "URGENT your bank account will be blocked complete KYC now"
        img_bytes = create_image_with_text(text, fmt="PNG")
        resp = client.post(
            "/analyze/image",
            files={"image": ("scam_hi.png", img_bytes, "image/png")},
            data={"language": "hi"},
        )
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertTrue(len(data["reasons"]) > 0)
        # Hindi reasons should NOT be English (basic sanity)
        # They should contain Hindi Unicode characters if signals were found
        if data["signals"]:
            for reason in data["reasons"]:
                # At minimum, it should not be the signal key itself
                self.assertNotEqual(reason, data["signals"][0])

    # ------------------------------------------------------------------
    # 6. Blank image – no readable text → 400
    # ------------------------------------------------------------------
    def test_blank_image_no_text(self):
        img_bytes = create_blank_image(fmt="PNG")
        resp = client.post(
            "/analyze/image",
            files={"image": ("blank.png", img_bytes, "image/png")},
            data={"language": "en"},
        )
        self.assertEqual(resp.status_code, 400)
        self.assertIn("OCR could not extract any text", resp.json()["detail"])


class TestOCRValidation(unittest.TestCase):
    """Validation tests that do NOT require Tesseract (format/size checks)."""

    # ------------------------------------------------------------------
    # 7. Unsupported format → 400
    # ------------------------------------------------------------------
    def test_unsupported_format(self):
        resp = client.post(
            "/analyze/image",
            files={"image": ("test.txt", b"hello world", "text/plain")},
            data={"language": "en"},
        )
        self.assertEqual(resp.status_code, 400)
        self.assertIn("Unsupported image format", resp.json()["detail"])

    # ------------------------------------------------------------------
    # 8. Image > 10 MB → 400
    # ------------------------------------------------------------------
    def test_large_image_rejected(self):
        # Create a minimal valid PNG header + enough padding to exceed 10 MB
        # We use raw bytes rather than generating a real huge image (faster)
        img = Image.new("RGB", (100, 100), color="white")
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        small_png = buf.getvalue()
        # Pad to just over 10 MB
        padded = small_png + b"\x00" * (10 * 1024 * 1024 + 1)
        resp = client.post(
            "/analyze/image",
            files={"image": ("huge.png", padded, "image/png")},
            data={"language": "en"},
        )
        self.assertEqual(resp.status_code, 400)
        self.assertIn("Image is too large", resp.json()["detail"])

    # ------------------------------------------------------------------
    # 9. Empty upload → 400
    # ------------------------------------------------------------------
    def test_empty_image(self):
        resp = client.post(
            "/analyze/image",
            files={"image": ("empty.png", b"", "image/png")},
            data={"language": "en"},
        )
        self.assertEqual(resp.status_code, 400)


if __name__ == "__main__":
    unittest.main()
