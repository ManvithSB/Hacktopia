"""Real local OCR engine using pytesseract + Tesseract.

This module performs actual pixel-based OCR on uploaded images.
It does NOT use image metadata, hardcoded strings, or external APIs.

Requirements:
    - Python package: pytesseract (pip install pytesseract)
    - System binary: Tesseract OCR (https://github.com/UB-Mannheim/tesseract/wiki)
"""

import io
from typing import Optional

import pytesseract
from PIL import Image
import os

# Configure Tesseract path for Windows if not in PATH
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

# Supported image MIME types
SUPPORTED_TYPES = {
    "image/png": "PNG",
    "image/jpeg": "JPEG",
    "image/jpg": "JPEG",
    "image/webp": "WEBP",
}

MAX_SIZE = 10 * 1024 * 1024  # 10 MB


def _load_image(image_bytes: bytes) -> Image.Image:
    """Load image from bytes while enforcing size limit.

    Raises ValueError if the image is too large or cannot be opened.
    """
    if len(image_bytes) > MAX_SIZE:
        raise ValueError("Image is too large. Maximum allowed size is 10 MB.")
    try:
        img = Image.open(io.BytesIO(image_bytes))
        # Force load so corrupt images fail here, not later
        img.load()
        return img
    except Exception as e:
        raise ValueError(f"Unable to open image: {e}")


def extract_text_from_image(image_bytes: bytes, language: Optional[str] = "en") -> str:
    """Extract text from image pixels using Tesseract OCR.

    Parameters:
        image_bytes: Raw image file data (PNG, JPEG, or WEBP).
        language: Language code (kept for API compatibility; Tesseract
                  uses its own language packs configured at system level).

    Returns:
        Extracted plain text stripped of leading/trailing whitespace.

    Raises:
        ValueError: If image is too large, unreadable, or OCR extracts no text.
        RuntimeError: If the Tesseract binary is not installed or not found.
    """
    img = _load_image(image_bytes)

    # Convert to RGB if necessary (e.g. RGBA PNGs, palette modes)
    if img.mode not in ("RGB", "L"):
        img = img.convert("RGB")

    try:
        raw_text = pytesseract.image_to_string(img)
    except pytesseract.TesseractNotFoundError:
        raise RuntimeError(
            "Tesseract OCR engine is not installed or not found on PATH. "
            "Install it from: https://github.com/UB-Mannheim/tesseract/wiki "
            "and ensure tesseract.exe is on your system PATH."
        )

    text = raw_text.strip()
    if not text:
        raise ValueError("OCR could not extract any text from the image.")

    return text
