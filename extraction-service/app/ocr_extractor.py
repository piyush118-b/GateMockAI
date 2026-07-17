"""
OCR Extractor — uses PaddleOCR to extract text from scanned/image-based PDFs.

Converts each PDF page to an image first (using PyMuPDF), then runs PaddleOCR.
This is Sub-Pipeline A for scanned PDFs.
"""

import fitz  # PyMuPDF — for page-to-image conversion
import logging
import numpy as np
from typing import List, Optional

logger = logging.getLogger(__name__)

# Lazy-load PaddleOCR to avoid slow startup when not needed
_ocr_engine = None


def get_ocr_engine():
    """Lazy-initializes the PaddleOCR engine (English, no GPU)."""
    global _ocr_engine
    if _ocr_engine is None:
        try:
            from paddleocr import PaddleOCR
            _ocr_engine = PaddleOCR(
                use_angle_cls=True,
                lang="en",
                use_gpu=False,
                show_log=False,
            )
            logger.info("PaddleOCR engine initialized successfully.")
        except ImportError:
            logger.error("PaddleOCR not available. Install paddleocr and paddlepaddle.")
            raise
    return _ocr_engine


def pdf_page_to_image_bytes(pdf_bytes: bytes, page_num: int, dpi: int = 200) -> bytes:
    """
    Renders a single PDF page to PNG bytes at the specified DPI.

    Args:
        pdf_bytes: raw PDF bytes
        page_num:  0-indexed page number
        dpi:       rendering resolution (higher = better OCR, slower)

    Returns:
        PNG bytes of the rendered page
    """
    doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    page = doc[page_num]
    mat = fitz.Matrix(dpi / 72, dpi / 72)  # 72 DPI is PyMuPDF's base resolution
    pix = page.get_pixmap(matrix=mat, colorspace=fitz.csRGB)
    img_bytes = pix.tobytes("png")
    doc.close()
    return img_bytes


def ocr_image_bytes(img_bytes: bytes) -> str:
    """
    Runs PaddleOCR on raw PNG/JPG bytes.
    Returns the extracted text as a single string.
    """
    import cv2
    nparr = np.frombuffer(img_bytes, np.uint8)
    img_array = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    if img_array is None:
        logger.warning("cv2.imdecode failed — image may be corrupt.")
        return ""

    ocr = get_ocr_engine()
    result = ocr.ocr(img_array, cls=True)

    if not result or result[0] is None:
        return ""

    lines = []
    for line in result[0]:
        if line and len(line) >= 2:
            text = line[1][0]  # OCR text
            confidence = line[1][1]
            if confidence > 0.5:
                lines.append(text)

    return "\n".join(lines)


def extract_text_with_ocr(pdf_bytes: bytes, dpi: int = 200) -> List[str]:
    """
    Extracts text from all pages of a scanned PDF using PaddleOCR.

    Returns:
        List of text strings, one per page
    """
    doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    total_pages = len(doc)
    doc.close()

    page_texts = []
    for page_num in range(total_pages):
        try:
            logger.debug(f"OCR processing page {page_num + 1}/{total_pages}")
            img_bytes = pdf_page_to_image_bytes(pdf_bytes, page_num, dpi=dpi)
            text = ocr_image_bytes(img_bytes)
            page_texts.append(text)
        except Exception as e:
            logger.error(f"OCR failed for page {page_num + 1}: {e}")
            page_texts.append("")

    return page_texts
