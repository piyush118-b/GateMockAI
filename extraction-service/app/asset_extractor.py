"""
Asset Extractor — extracts embedded images from questions and uploads them to MinIO.

For each question block, this module:
  1. Identifies which PDF page(s) the question appears on
  2. Extracts images from those pages using PyMuPDF
  3. Uploads images to MinIO using standardized object keys
  4. Returns ExtractedAsset references (no binary data stored in DB)

This is Step 7-8 of Sub-Pipeline A.
"""

import hashlib
import logging
from typing import List, Optional
from .models import ExtractedAsset
from .minio_client import upload_image, question_image_key, option_image_key
from .digital_extractor import get_image_dimensions

logger = logging.getLogger(__name__)


def compute_sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def classify_asset_type(width: int, height: int, context_hint: str = "") -> str:
    """
    Heuristically classifies an extracted image as a specific asset type.
    GATE papers typically contain: graphs, trees, automata, tables, circuits.
    """
    hint = context_hint.lower()
    if any(kw in hint for kw in ["graph", "plot", "axis", "chart"]):
        return "Graph"
    if any(kw in hint for kw in ["tree", "node", "root", "child", "leaf"]):
        return "Tree"
    if any(kw in hint for kw in ["automata", "state", "transition", "nfa", "dfa"]):
        return "Automata"
    if any(kw in hint for kw in ["circuit", "gate", "flip-flop", "logic"]):
        return "Circuit"
    if any(kw in hint for kw in ["table", "row", "column"]):
        return "Table"
    if any(kw in hint for kw in ["flow", "algorithm", "step"]):
        return "Flowchart"
    # Classify wide images as tables, tall images as graphs
    if width > 0 and height > 0:
        ratio = width / height
        if ratio > 2.5:
            return "Table"
        if ratio < 0.5:
            return "Graph"
    return "Image"


def extract_and_upload_question_assets(
    page_images: List[bytes],  # PNG bytes for each image found on the question's page(s)
    paper_id: str,
    question_id: str,
    question_text: str = "",
) -> List[ExtractedAsset]:
    """
    Uploads question-level assets (images) to MinIO and returns references.

    Args:
        page_images: list of PNG byte arrays extracted from the question's PDF pages
        paper_id:    GATE paper ID
        question_id: e.g., "gate_cse_2020_Q17"
        question_text: used for asset type classification hints

    Returns:
        List of ExtractedAsset with MinIO object keys (no binary data)
    """
    assets: List[ExtractedAsset] = []

    for idx, img_bytes in enumerate(page_images):
        try:
            checksum = compute_sha256(img_bytes)
            width, height = get_image_dimensions(img_bytes)
            asset_type = classify_asset_type(width, height, question_text)
            filename = f"asset_{idx + 1}.png"
            object_key = question_image_key(paper_id, question_id, filename)

            upload_image(img_bytes, object_key, mime_type="image/png")

            asset = ExtractedAsset(
                asset_type=asset_type,
                object_key=object_key,
                mime_type="image/png",
                width=width,
                height=height,
                checksum=checksum,
            )
            assets.append(asset)
            logger.debug(f"Uploaded asset: {object_key} ({asset_type}, {width}x{height})")

        except Exception as e:
            logger.error(f"Failed to upload asset {idx} for question {question_id}: {e}")

    return assets


def extract_and_upload_option_assets(
    option_images: List[bytes],
    paper_id: str,
    question_id: str,
    option_label: str,
) -> List[ExtractedAsset]:
    """Uploads option-level assets (image options) to MinIO."""
    assets: List[ExtractedAsset] = []

    for idx, img_bytes in enumerate(option_images):
        try:
            checksum = compute_sha256(img_bytes)
            width, height = get_image_dimensions(img_bytes)
            filename = f"option_{option_label}_{idx + 1}.png"
            object_key = option_image_key(paper_id, question_id, option_label, filename)

            upload_image(img_bytes, object_key, mime_type="image/png")

            asset = ExtractedAsset(
                asset_type="Image",
                object_key=object_key,
                mime_type="image/png",
                width=width,
                height=height,
                checksum=checksum,
            )
            assets.append(asset)

        except Exception as e:
            logger.error(f"Failed to upload option asset for {question_id}/{option_label}: {e}")

    return assets
