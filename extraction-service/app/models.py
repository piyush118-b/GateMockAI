"""
Pydantic models for the GATE Extraction Microservice.

These models define the contract between the Python extraction service
and the Java Spring Boot application.
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict


class ExtractedAsset(BaseModel):
    """Reference to an asset uploaded to MinIO."""
    asset_type: str = "Image"           # Image, Graph, Table, Circuit, Tree, Automata, Flowchart
    object_key: str                      # MinIO object key
    mime_type: str = "image/png"
    width: Optional[int] = None
    height: Optional[int] = None
    checksum: Optional[str] = None      # SHA-256 hex digest


class ExtractedOption(BaseModel):
    """A single answer option for MCQ/MSQ."""
    label: str                           # A, B, C, D
    option_text: str
    assets: List[ExtractedAsset] = []   # Option-level images


class ExtractedQuestion(BaseModel):
    """One fully parsed GATE question."""
    question_number: int
    section: str = "GA"                 # GA or CS (or Section A/B for other exams)
    question_type: str = "MCQ"          # MCQ, MSQ, NAT
    question_text: str
    marks: Optional[float] = None
    negative_marks: Optional[float] = None
    options: List[ExtractedOption] = []
    assets: List[ExtractedAsset] = []   # Question-level images/graphs
    valid: bool = True
    validation_message: Optional[str] = None


class ExtractedPaper(BaseModel):
    """Result of Sub-Pipeline A: question paper extraction."""
    exam_name: str
    year: Optional[int] = None
    branch: Optional[str] = None
    session: Optional[str] = None
    duration: Optional[int] = None
    total_marks: Optional[float] = None
    total_questions: Optional[int] = None
    pdf_type: str = "digital"           # "digital" or "scanned"
    questions: List[ExtractedQuestion] = []


class ExtractedAnswerKey(BaseModel):
    """Result of Sub-Pipeline B: answer key extraction."""
    answers: Dict[str, str] = {}        # {question_number_str → answer_string}
    pdf_type: str = "digital"
    total_parsed: int = 0
    parser_warnings: Optional[str] = None


class ExtractionRequest(BaseModel):
    """Parameters for a paper extraction request."""
    paper_id: str
    exam_name: Optional[str] = "GATE"
    year: Optional[int] = None
    branch: Optional[str] = "CSE"
