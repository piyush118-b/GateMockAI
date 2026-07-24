"""
GateMockAI v2.1 — Page Prep & Diagram Crop Service

Lightweight FastAPI service. In v2.1, Gemini reads the PDF directly for
extraction + solving + enrichment. This service only provides:
  POST /crop-diagram  — crops a bounding-box region from a page image → WebP
  GET  /health        — health check
  GET  /info          — service metadata

Dependencies: Pillow, pdf2image (NO OCR, NO PaddleOCR, NO PyMuPDF)
"""

import base64
import io
import logging
from typing import Optional

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from PIL import Image

from .models import DiagramCropRequest, DiagramCropResponse, ErrorResponse

# ── Logging ──────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

# ── App ───────────────────────────────────────────────────────────────────────
app = FastAPI(
    title="GateMockAI — Diagram Crop Service",
    description=(
        "v2.1: Lightweight diagram crop helper. "
        "Gemini reads PDFs directly; this service only crops bounding boxes to WebP."
    ),
    version="2.1.0",
)


# ─────────────────────────────────────────────────────────────────────────────
# POST /crop-diagram
# ─────────────────────────────────────────────────────────────────────────────

@app.post("/crop-diagram", response_model=DiagramCropResponse)
async def crop_diagram(request: DiagramCropRequest):
    """
    Crops a diagram region from a page image.

    Input:
      - image_base64: base64-encoded PNG of the full page
      - bounding_box: Gemini's normalized coordinates (0-1000 grid)
      - padding_px: optional extra padding (default 8px)

    Output:
      - webp_base64: base64-encoded WebP of the cropped region
      - width_px / height_px: dimensions of the crop
    """
    try:
        # Decode input image
        img_bytes = base64.b64decode(request.image_base64)
        page_img = Image.open(io.BytesIO(img_bytes)).convert("RGB")
        img_w, img_h = page_img.size

        # Convert normalized 0-1000 coordinates → pixel coordinates
        bb = request.bounding_box
        x_min_px = int(bb.x_min / 1000 * img_w) - request.padding_px
        y_min_px = int(bb.y_min / 1000 * img_h) - request.padding_px
        x_max_px = int(bb.x_max / 1000 * img_w) + request.padding_px
        y_max_px = int(bb.y_max / 1000 * img_h) + request.padding_px

        # Clamp to image bounds
        x_min_px = max(0, x_min_px)
        y_min_px = max(0, y_min_px)
        x_max_px = min(img_w, x_max_px)
        y_max_px = min(img_h, y_max_px)

        if x_max_px <= x_min_px or y_max_px <= y_min_px:
            raise HTTPException(status_code=400, detail="Invalid bounding box: zero-area crop")

        # Crop the region
        cropped = page_img.crop((x_min_px, y_min_px, x_max_px, y_max_px))

        # Encode as WebP (lossless=False, quality=85 — good balance)
        out_buffer = io.BytesIO()
        cropped.save(out_buffer, format="WEBP", quality=85, method=6)
        webp_bytes = out_buffer.getvalue()
        webp_b64 = base64.b64encode(webp_bytes).decode("utf-8")

        logger.info(
            "Cropped diagram: input=%dx%d bbox=(%d,%d,%d,%d) output=%dx%d (%d bytes WebP)",
            img_w, img_h, x_min_px, y_min_px, x_max_px, y_max_px,
            cropped.width, cropped.height, len(webp_bytes)
        )

        return DiagramCropResponse(
            webp_base64=webp_b64,
            width_px=cropped.width,
            height_px=cropped.height,
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error("Crop failed: %s", str(e), exc_info=True)
        raise HTTPException(status_code=500, detail=f"Crop failed: {str(e)}")


# ─────────────────────────────────────────────────────────────────────────────
# GET /health
# ─────────────────────────────────────────────────────────────────────────────

@app.get("/health")
async def health():
    return {"status": "ok", "service": "diagram-crop", "version": "2.1.0"}


# ─────────────────────────────────────────────────────────────────────────────
# GET /info
# ─────────────────────────────────────────────────────────────────────────────

@app.get("/info")
async def info():
    return {
        "service": "GateMockAI Diagram Crop Service",
        "version": "2.1.0",
        "description": (
            "Lightweight bounding-box crop helper. "
            "Gemini reads PDFs directly in v2.1; this service only crops diagram regions."
        ),
        "endpoints": [
            "POST /crop-diagram — crop bounding box → WebP",
            "GET  /health",
            "GET  /info",
        ],
        "removed_in_v2_1": [
            "POST /extract/question-paper (replaced by Gemini native PDF read)",
            "POST /extract/answer-key (answer keys removed entirely)",
        ],
    }
