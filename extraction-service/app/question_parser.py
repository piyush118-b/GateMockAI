"""
Question Parser — parses raw text blocks into structured GateQuestion objects.

Uses regex-based boundary detection to identify:
  - Question numbers (1., 2., Q.1, Q1, etc.)
  - Question type (MCQ, MSQ, NAT keywords)
  - Marks (1 Mark, 2 Marks, [1], [2])
  - Sections (GA, CS, General Aptitude, Computer Science)

This is Step 3-5 of Sub-Pipeline A.
"""

import re
import logging
from typing import List, Optional, Tuple
from .models import ExtractedQuestion, ExtractedOption

logger = logging.getLogger(__name__)

# ─── Regex Patterns ─────────────────────────────────────────────────────────

# Matches: "Q.1", "Q1.", "1.", "Question 1", "(1)"
QUESTION_START = re.compile(
    r'(?:^|\n)\s*(?:Q\.?\s*|Question\s+)?(\d+)[.)]\s+',
    re.MULTILINE | re.IGNORECASE
)

# Matches option lines: "(A)", "A.", "A)", "(a)"
OPTION_PATTERN = re.compile(
    r'^\s*[(\[]?\s*([A-Da-d])\s*[.)\]]\s+(.+)',
    re.MULTILINE
)

# Marks patterns
MARKS_1 = re.compile(r'\b1[\s-]*[Mm]ark\b|\[1\]|\(1\)')
MARKS_2 = re.compile(r'\b2[\s-]*[Mm]arks?\b|\[2\]|\(2\)')

# Section detection
GA_SECTION = re.compile(
    r'general\s+aptitude|section\s*[-:]?\s*ga|^GA\b',
    re.IGNORECASE | re.MULTILINE
)
CS_SECTION = re.compile(
    r'computer\s+science|section\s*[-:]?\s*cs|^CS\b',
    re.IGNORECASE | re.MULTILINE
)

# NAT question indicators
NAT_PATTERN = re.compile(
    r'\b(NAT|Numerical\s+Answer|fill\s+in\s+the\s+blank|integer)\b',
    re.IGNORECASE
)

# MSQ question indicators
MSQ_PATTERN = re.compile(
    r'\b(MSQ|Multiple\s+Select|one\s+or\s+more|all\s+that\s+apply)\b',
    re.IGNORECASE
)

# Negative marks by type
NEG_MARKS = {"MCQ_1": 0.33, "MCQ_2": 0.67, "MSQ": 0.0, "NAT": 0.0}


def detect_section(text: str, current_section: str = "GA") -> str:
    """Detects the current section from text headers."""
    if GA_SECTION.search(text):
        return "GA"
    if CS_SECTION.search(text):
        return "CS"
    return current_section


def detect_question_type(text: str) -> str:
    """Infers MCQ, MSQ, or NAT from question text clues."""
    if NAT_PATTERN.search(text):
        return "NAT"
    if MSQ_PATTERN.search(text):
        return "MSQ"
    return "MCQ"


def detect_marks(text: str, question_number: int, section: str) -> Tuple[float, float]:
    """
    Returns (marks, negative_marks) based on GATE standard marking scheme.
    GA: Q1-5 = 1 mark, Q6-10 = 2 marks
    CS: Q1-25 = 1 mark, Q26-55 = 2 marks
    """
    if MARKS_2.search(text):
        marks = 2.0
    elif MARKS_1.search(text):
        marks = 1.0
    else:
        # Infer from question number and section
        if section == "GA":
            marks = 1.0 if question_number <= 5 else 2.0
        else:
            marks = 1.0 if question_number <= 25 else 2.0

    q_type = detect_question_type(text)
    if q_type == "MCQ":
        neg = 0.33 if marks == 1.0 else 0.67
    else:
        neg = 0.0

    return marks, neg


def parse_options(text: str) -> List[ExtractedOption]:
    """Parses option lines from question text."""
    options = []
    seen_labels = set()
    for match in OPTION_PATTERN.finditer(text):
        label = match.group(1).upper()
        option_text = match.group(2).strip()
        if label not in seen_labels and len(option_text) > 0:
            options.append(ExtractedOption(label=label, option_text=option_text))
            seen_labels.add(label)
    return options


def split_into_question_blocks(full_text: str) -> List[Tuple[int, str]]:
    """
    Splits the full PDF text into (question_number, question_block) tuples.
    Each block contains the raw text for one question.
    """
    matches = list(QUESTION_START.finditer(full_text))
    if not matches:
        logger.warning("No question boundaries detected in text.")
        return []

    blocks = []
    for i, match in enumerate(matches):
        q_num = int(match.group(1))
        start = match.start()
        end = matches[i + 1].start() if i + 1 < len(matches) else len(full_text)
        block_text = full_text[start:end].strip()
        blocks.append((q_num, block_text))

    return blocks


def parse_questions(full_text: str, paper_id: str) -> List[ExtractedQuestion]:
    """
    Main entry point: parses raw PDF text into a list of ExtractedQuestion objects.
    """
    current_section = "GA"
    questions: List[ExtractedQuestion] = []

    blocks = split_into_question_blocks(full_text)
    logger.info(f"Detected {len(blocks)} question blocks in text.")

    for q_num, block in blocks:
        try:
            # Update section context
            current_section = detect_section(block, current_section)
            q_type = detect_question_type(block)
            marks, neg_marks = detect_marks(block, q_num, current_section)
            options = parse_options(block) if q_type in ("MCQ", "MSQ") else []

            # Clean up question text: remove option lines from question body
            q_text = re.sub(OPTION_PATTERN, "", block).strip()
            q_text = re.sub(r'\s{3,}', ' ', q_text)  # Collapse excessive whitespace

            # Basic validity check
            valid = len(q_text) >= 10
            validation_msg = None if valid else f"Question {q_num} text too short"

            question = ExtractedQuestion(
                question_number=q_num,
                section=current_section,
                question_type=q_type,
                question_text=q_text,
                marks=marks,
                negative_marks=neg_marks,
                options=options,
                valid=valid,
                validation_message=validation_msg,
            )
            questions.append(question)
            logger.debug(f"Parsed Q{q_num}: type={q_type}, section={current_section}, marks={marks}, options={len(options)}")

        except Exception as e:
            logger.error(f"Failed to parse question block for Q{q_num}: {e}")
            questions.append(ExtractedQuestion(
                question_number=q_num,
                section=current_section,
                question_type="MCQ",
                question_text=block[:200],
                valid=False,
                validation_message=str(e),
            ))

    return questions
