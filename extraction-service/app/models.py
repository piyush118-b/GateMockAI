"""
v2.1 Data Models — DiagramCropService only.

The old ExtractedPaper, ExtractedQuestion, ExtractedAnswerKey models are removed.
Gemini (via Spring Boot) now reads the PDF directly and extracts all question data.
This Python service only handles lightweight diagram image cropping.
"""

from pydantic import BaseModel, Field
from typing import Optional


class BoundingBox(BaseModel):
    """Normalized bounding box coordinates on a 0-1000 grid."""
    y_min: float = Field(ge=0, le=1000, description="Top edge (0-1000)")
    x_min: float = Field(ge=0, le=1000, description="Left edge (0-1000)")
    y_max: float = Field(ge=0, le=1000, description="Bottom edge (0-1000)")
    x_max: float = Field(ge=0, le=1000, description="Right edge (0-1000)")


class DiagramCropRequest(BaseModel):
    """Request to crop a diagram from a page image."""
    image_base64: str = Field(description="Base64-encoded PNG of the page")
    bounding_box: BoundingBox = Field(description="Normalized bounding box from Gemini")
    padding_px: int = Field(default=8, ge=0, le=50, description="Extra padding around the crop")


class DiagramCropResponse(BaseModel):
    """Response from the crop service."""
    webp_base64: str = Field(description="Base64-encoded WebP of the cropped diagram")
    width_px: int
    height_px: int
    success: bool = True
    message: str = "OK"


class ErrorResponse(BaseModel):
    success: bool = False
    message: str
