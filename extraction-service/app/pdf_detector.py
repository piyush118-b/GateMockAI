"""
PDF Type Detector — determines whether a PDF is digital (text-selectable) or scanned (image-only).

Uses PyMuPDF (fitz) to detect the presence of selectable text.
A PDF is classified as "digital" if it has more than MIN_TEXT_CHARS of extractable text
across its first N pages, otherwise "scanned".
"""

import fitz  # PyMuPDF
import logging

logger = logging.getLogger(__name__)

# Minimum characters per page to classify as digital
MIN_CHARS_PER_PAGE = 300
SAMPLE_PAGES = 5  # Only check first N pages for performance


def detect_pdf_type(pdf_bytes: bytes) -> str:
    """
    Detects whether a PDF is digital (selectable text) or scanned (images only).

    Returns:
        "digital" — text-selectable PDF, use PyMuPDF extraction
        "scanned" — image-based PDF, use PaddleOCR pipeline
    """
    try:
        doc = fitz.open(stream=pdf_bytes, filetype="pdf")
        total_pages = len(doc)
        pages_to_check = min(SAMPLE_PAGES, total_pages)
        total_chars = 0

        for page_num in range(pages_to_check):
            page = doc[page_num]
            text = page.get_text("text").strip()
            total_chars += len(text)

        doc.close()

        avg_chars = total_chars / max(pages_to_check, 1)
        pdf_type = "digital" if avg_chars >= MIN_CHARS_PER_PAGE else "scanned"

        logger.info(
            f"PDF type detection: avg_chars_per_page={avg_chars:.0f} → type={pdf_type} "
            f"(total_pages={total_pages}, sampled={pages_to_check})"
        )
        return pdf_type

    except Exception as e:
        logger.error(f"PDF type detection failed: {e}. Defaulting to 'digital'.")
        return "digital"
