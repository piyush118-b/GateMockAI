"""
Answer Key Parser — Sub-Pipeline B.

Parses official GATE answer key PDFs and text files into a structured dictionary.

Supported formats:
  - Table format: "1  MCQ  GA  D  1"
  - Prefixed:     "GA 1: D" / "CS 17: 0.125 to 0.125"
  - Simple list:  "1: A" or "1 - D"
  - NAT range:    "17: 15.2 to 15.4"
  - MSQ:          "22: A, C"
"""

import re
import logging
from typing import Dict, Optional

logger = logging.getLogger(__name__)

# ─── Regex Patterns ────────────────────────────────────────────────────────

# Table row: "1  6  MCQ  GA  D  1" or similar
TABLE_ROW = re.compile(
    r'^\s*(\d+)\s+\S+\s+(MCQ|NAT|MSQ)\s+(GA|CS)\s+(.+?)\s+(\d+(?:\.\d+)?)\s*$',
    re.IGNORECASE
)

# Section-prefixed: "GA 1: D" or "CS 17: 15.2 to 15.4"
PREFIXED = re.compile(
    r'^\s*(GA|CS)\s*[-_]?\s*(?:Q)?\s*(\d+)\s*[:=-]\s*(.+)$',
    re.IGNORECASE
)

# Simple: "1: D" or "1 - A" or "1. A"
SIMPLE = re.compile(r'^\s*(\d+)\s*[:=.\-]\s*(.+)$')

# Section headers
GA_HEADER = re.compile(r'general\s+aptitude|section\s*[-:]?\s*ga', re.IGNORECASE)
CS_HEADER = re.compile(r'computer\s+science|section\s*[-:]?\s*cs', re.IGNORECASE)


def normalize_answer(raw: str) -> str:
    """Normalizes an answer string for consistent storage."""
    ans = raw.strip()
    # Remove trailing punctuation
    ans = re.sub(r'[,;.]$', '', ans)
    # Normalize MSQ: "A,C" → "A,C" (uppercase, no spaces)
    if re.match(r'^[A-Da-d](,\s*[A-Da-d])+$', ans):
        parts = [p.strip().upper() for p in ans.split(',')]
        return ",".join(parts)
    # Uppercase MCQ single letter
    if re.match(r'^[A-Da-d]$', ans.strip()):
        return ans.strip().upper()
    return ans


def parse_answer_key(text: str) -> Dict[str, str]:
    """
    Parses answer key text into a dictionary of {question_number_str → answer}.

    Returns:
        Dict e.g. {"1": "A", "2": "D", "17": "15.2 to 15.4", "22": "A,C"}
    """
    answer_map: Dict[str, str] = {}
    lines = text.split('\n')
    current_section = "GA"

    for line in lines:
        trimmed = line.strip()
        if not trimmed:
            continue

        # Detect section change
        if GA_HEADER.search(trimmed):
            current_section = "GA"
            logger.debug("Switched to GA section")
            continue
        if CS_HEADER.search(trimmed):
            current_section = "CS"
            logger.debug("Switched to CS section")
            continue

        # Try table row format
        m = TABLE_ROW.match(trimmed)
        if m:
            q_num = m.group(1)
            section = m.group(3).upper()
            answer = normalize_answer(m.group(4))
            answer_map[q_num] = answer
            logger.debug(f"Table row: Q{q_num} ({section}) → {answer}")
            continue

        # Try prefixed format
        m = PREFIXED.match(trimmed)
        if m:
            section = m.group(1).upper()
            q_num = m.group(2)
            answer = normalize_answer(m.group(3))
            answer_map[q_num] = answer
            logger.debug(f"Prefixed: Q{q_num} ({section}) → {answer}")
            continue

        # Try simple format
        m = SIMPLE.match(trimmed)
        if m:
            q_num = m.group(1)
            answer = normalize_answer(m.group(2))
            answer_map[q_num] = answer
            logger.debug(f"Simple: Q{q_num} → {answer}")
            continue

    logger.info(f"Parsed {len(answer_map)} answers from answer key text.")
    return answer_map
