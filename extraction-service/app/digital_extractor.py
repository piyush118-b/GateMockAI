"""
Digital PDF Extractor — uses PyMuPDF (fitz) to extract text, images, and layout
from text-selectable (digital) PDFs.

This is Sub-Pipeline A for digital PDFs.
"""

import fitz  # PyMuPDF
import re
import io
import logging
from typing import List, Tuple, Dict, Optional
from PIL import Image

logger = logging.getLogger(__name__)


def extract_text_by_page(pdf_bytes: bytes) -> List[str]:
    """Extracts text from each page of a digital PDF."""
    doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    pages = []
    for page in doc:
        text = page.get_text("text")
        pages.append(text)
    doc.close()
    return pages


def extract_full_text(pdf_bytes: bytes) -> str:
    """Extracts all text from a digital PDF as a single string."""
    pages = extract_text_by_page(pdf_bytes)
    return "\n\n".join(pages)


def extract_images_from_page(page: fitz.Page, min_width: int = 30, min_height: int = 30) -> List[bytes]:
    """
    Extracts all embedded images from a PDF page.
    Returns a list of PNG bytes for each image found.
    """
    images = []
    img_list = page.get_images(full=True)

    for img_index, img_info in enumerate(img_list):
        xref = img_info[0]
        try:
            base_image = page.parent.extract_image(xref)
            img_bytes = base_image["image"]
            img_ext = base_image.get("ext", "png")
            width = base_image.get("width", 0)
            height = base_image.get("height", 0)

            # Filter out tiny images (icons, bullets, etc.)
            if width < min_width or height < min_height:
                continue

            # Convert to PNG for uniformity
            if img_ext.lower() not in ("png",):
                pil_img = Image.open(io.BytesIO(img_bytes))
                out = io.BytesIO()
                pil_img.save(out, format="PNG")
                img_bytes = out.getvalue()

            images.append(img_bytes)
        except Exception as e:
            logger.warning(f"Failed to extract image xref={xref}: {e}")

    return images


def extract_all_images(pdf_bytes: bytes) -> Dict[int, List[bytes]]:
    """
    Extracts images from all pages of a digital PDF.

    Returns:
        Dict mapping page_number (1-indexed) → list of PNG bytes
    """
    doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    result: Dict[int, List[bytes]] = {}

    for page_num in range(len(doc)):
        page = doc[page_num]
        imgs = extract_images_from_page(page)
        if imgs:
            result[page_num + 1] = imgs

    doc.close()
    return result


def get_image_dimensions(img_bytes: bytes) -> Tuple[int, int]:
    """Returns (width, height) of an image from its bytes."""
    try:
        img = Image.open(io.BytesIO(img_bytes))
        return img.size  # (width, height)
    except Exception:
        return (0, 0)
