"""
GATE Extraction Microservice — FastAPI Application

Pipeline 1 Document Extraction Service.

Provides:
  POST /extract/question-paper  → Sub-Pipeline A: extract questions from PDF
  POST /extract/answer-key      → Sub-Pipeline B: extract answers from PDF/text
  GET  /health                  → Health check
  GET  /info                    → Service metadata

This service uses:
  - PyMuPDF for digital PDFs (text + image extraction)
  - PaddleOCR for scanned PDFs
  - MinIO for asset storage

NO AI-generated information is produced here.
All extracted data is official information from the source PDFs.
"""

import logging
import traceback
from typing import Optional

from fastapi import FastAPI, File, Form, UploadFile, HTTPException
from fastapi.responses import JSONResponse

from .models import ExtractedPaper, ExtractedAnswerKey
from .pdf_detector import detect_pdf_type
from .digital_extractor import extract_full_text, extract_all_images
from .ocr_extractor import extract_text_with_ocr
from .question_parser import parse_questions
from .asset_extractor import extract_and_upload_question_assets
from .answer_key_parser import parse_answer_key
from . import minio_client

# ─── Logging ─────────────────────────────────────────────────────────────────

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

# ─── FastAPI App ──────────────────────────────────────────────────────────────

app = FastAPI(
    title="GATE Extraction Microservice",
    description="Pipeline 1: Document Extraction Service for GATE Question Papers and Answer Keys",
    version="1.0.0",
)

from fastapi import Request

@app.middleware("http")
async def log_requests(request: Request, call_next):
    print(f"Incoming request: {request.method} {request.url}", flush=True)
    print(f"Headers: {dict(request.headers)}", flush=True)
    try:
        response = await call_next(request)
        print(f"Response status: {response.status_code}", flush=True)
        return response
    except Exception as e:
        print(f"Middleware caught error: {e}", flush=True)
        raise

# ─── Health Check ─────────────────────────────────────────────────────────────

@app.get("/health")
async def health():
    """Health check endpoint for Docker and Spring Boot readiness probes."""
    return {"status": "healthy", "service": "gate-extractor", "version": "1.0.0"}


@app.get("/info")
async def info():
    """Service metadata and capabilities."""
    return {
        "service": "gate-extractor",
        "version": "1.0.0",
        "pipelines": {
            "sub_pipeline_a": "POST /extract/question-paper",
            "sub_pipeline_b": "POST /extract/answer-key",
        },
        "supported_formats": ["digital_pdf", "scanned_pdf"],
        "ocr_engine": "PaddleOCR",
        "pdf_library": "PyMuPDF",
        "storage": "MinIO",
    }


# ─── Sub-Pipeline A: Question Paper Extraction ────────────────────────────────

@app.post("/extract/question-paper", response_model=ExtractedPaper)
async def extract_question_paper(
    file: UploadFile = File(..., description="Question paper PDF file"),
    paper_id: str = Form(..., description="Unique identifier for this paper, e.g. gate_cse_2020"),
    exam_name: Optional[str] = Form("GATE", description="Exam name, e.g. GATE CSE 2020"),
    year: Optional[int] = Form(None, description="Exam year"),
    branch: Optional[str] = Form("CSE", description="Branch/stream"),
):
    """
    Sub-Pipeline A: Extracts all questions from an official GATE question paper PDF.

    Steps:
    1. Detect PDF type (digital or scanned)
    2. Extract text using appropriate method
    3. Detect question boundaries and parse questions
    4. Extract and upload images/assets to MinIO
    5. Return structured ExtractedPaper with all questions and asset references

    NO AI is used in this endpoint.
    """
    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only PDF files are supported.")

    logger.info(f"[Pipeline 1A] Starting extraction for paper_id={paper_id}, exam={exam_name}")

    try:
        pdf_bytes = await file.read()
        logger.info(f"[Pipeline 1A] Read {len(pdf_bytes)} bytes from uploaded PDF")

        # Step 1: Upload original PDF to MinIO
        try:
            pdf_key = minio_client.paper_pdf_key(paper_id, "original_paper.pdf")
            minio_client.upload_pdf(pdf_bytes, pdf_key)
            logger.info(f"[Pipeline 1A] Original PDF uploaded to MinIO: {pdf_key}")
        except Exception as e:
            logger.warning(f"[Pipeline 1A] Failed to upload original PDF to MinIO: {e}")

        # Step 2: Detect PDF type
        pdf_type = detect_pdf_type(pdf_bytes)
        logger.info(f"[Pipeline 1A] PDF type detected: {pdf_type}")

        # Step 3: Extract text
        if pdf_type == "digital":
            from .digital_extractor import extract_text_by_page
            pages = extract_text_by_page(pdf_bytes)
            filtered_pages = []
            for page_text in pages:
                normalized = page_text.lower()
                if "answer key" in normalized or "answer-key" in normalized or "answerkey" in normalized:
                    logger.info("[Pipeline 1A] Found Answer Key page. Discarding this and remaining pages.")
                    break
                filtered_pages.append(page_text)
            full_text = "\n\n".join(filtered_pages)
            
            # Extract images only for non-answer-key pages
            num_filtered_pages = len(filtered_pages)
            raw_images_map = extract_all_images(pdf_bytes)
            page_images_map = {k: v for k, v in raw_images_map.items() if k <= num_filtered_pages}
        else:
            # Scanned PDF — use OCR
            page_texts = extract_text_with_ocr(pdf_bytes)
            filtered_page_texts = []
            for page_text in page_texts:
                normalized = page_text.lower()
                if "answer key" in normalized or "answer-key" in normalized or "answerkey" in normalized:
                    logger.info("[Pipeline 1A] Found Answer Key page in OCR. Discarding this and remaining pages.")
                    break
                filtered_page_texts.append(page_text)
            full_text = "\n\n".join(filtered_page_texts)
            page_images_map = {}  # OCR doesn't extract embedded images separately

        logger.info(f"[Pipeline 1A] Extracted {len(full_text)} characters of text")

        # Step 4: Parse questions
        questions = parse_questions(full_text, paper_id)
        logger.info(f"[Pipeline 1A] Parsed {len(questions)} questions")

        # Step 5: Extract and upload assets per question
        # Map page images to questions (rough heuristic: distribute evenly)
        all_page_images = []
        for page_num in sorted(page_images_map.keys()):
            all_page_images.extend(page_images_map[page_num])

        # Simple allocation: assign images to questions proportionally
        images_per_question = max(1, len(all_page_images) // max(len(questions), 1))
        img_cursor = 0

        for q in questions:
            question_id = f"{paper_id}_Q{q.question_number}"
            q_images = all_page_images[img_cursor:img_cursor + images_per_question]
            img_cursor += images_per_question

            if q_images:
                assets = extract_and_upload_question_assets(
                    page_images=q_images,
                    paper_id=paper_id,
                    question_id=question_id,
                    question_text=q.question_text,
                )
                q.assets.extend(assets)

        # Derive paper-level metadata from exam_name
        inferred_year = year
        if not inferred_year and exam_name:
            import re
            year_match = re.search(r'\b(20\d{2})\b', exam_name)
            if year_match:
                inferred_year = int(year_match.group(1))

        result = ExtractedPaper(
            exam_name=exam_name or "GATE",
            year=inferred_year,
            branch=branch,
            pdf_type=pdf_type,
            total_questions=len(questions),
            questions=questions,
        )

        logger.info(f"[Pipeline 1A] Extraction complete: {len(questions)} questions for paper_id={paper_id}")
        return result

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"[Pipeline 1A] Extraction failed: {e}\n{traceback.format_exc()}")
        raise HTTPException(status_code=500, detail=f"Extraction failed: {str(e)}")


# ─── Sub-Pipeline B: Answer Key Extraction ───────────────────────────────────

@app.post("/extract/answer-key", response_model=ExtractedAnswerKey)
async def extract_answer_key(
    file: UploadFile = File(..., description="Answer key PDF or TXT file"),
    paper_id: str = Form(..., description="Paper ID matching the question paper"),
):
    """
    Sub-Pipeline B: Extracts official answers from an answer key PDF or text file.

    Supports:
    - Digital PDF with table-format answer keys
    - Scanned PDF answer keys (via OCR)
    - Plain text (.txt) answer keys

    Returns a normalized {question_number → answer} dictionary.
    """
    filename = file.filename.lower()
    logger.info(f"[Pipeline 1B] Starting answer key extraction for paper_id={paper_id}")

    try:
        file_bytes = await file.read()

        # Step 1: Upload original answer key to MinIO
        try:
            ak_key = minio_client.paper_pdf_key(paper_id, "answer_key.pdf")
            minio_client.upload_pdf(file_bytes, ak_key)
            logger.info(f"[Pipeline 1B] Answer key uploaded to MinIO: {ak_key}")
        except Exception as e:
            logger.warning(f"[Pipeline 1B] Failed to upload answer key to MinIO: {e}")

        # Step 2: Extract text
        if filename.endswith(".pdf"):
            pdf_type = detect_pdf_type(file_bytes)
            if pdf_type == "digital":
                from .digital_extractor import extract_full_text
                raw_text = extract_full_text(file_bytes)
            else:
                from .ocr_extractor import extract_text_with_ocr
                pages = extract_text_with_ocr(file_bytes)
                raw_text = "\n".join(pages)
        elif filename.endswith((".txt", ".csv")):
            raw_text = file_bytes.decode("utf-8", errors="replace")
            pdf_type = "text"
        else:
            raise HTTPException(status_code=400, detail="Only .pdf or .txt files are supported for answer keys.")

        # Step 3: Parse the answer key
        answers = parse_answer_key(raw_text)
        logger.info(f"[Pipeline 1B] Extracted {len(answers)} answers for paper_id={paper_id}")

        warnings = None
        if len(answers) == 0:
            warnings = "No answers could be parsed. Check file format."
        elif len(answers) < 10:
            warnings = f"Only {len(answers)} answers parsed. Check if format is supported."

        return ExtractedAnswerKey(
            answers=answers,
            pdf_type=pdf_type,
            total_parsed=len(answers),
            parser_warnings=warnings,
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"[Pipeline 1B] Answer key extraction failed: {e}\n{traceback.format_exc()}")
        raise HTTPException(status_code=500, detail=f"Answer key extraction failed: {str(e)}")
