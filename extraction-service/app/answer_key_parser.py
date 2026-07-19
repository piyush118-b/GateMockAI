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
    Parses answer key text into a dictionary of {section_question_number → answer}.
    Returns:
        Dict e.g. {"GA_1": "D", "CS_1": "A", "CS_17": "0.125 to 0.125"}
    """
    answer_map: Dict[str, str] = {}
    lines = [l.strip() for l in text.split('\n') if l.strip()]

    # 1. Try vertical layout parsing first (regular sequence in columns)
    for i in range(len(lines)):
        val = lines[i].upper()
        if val in ['MCQ', 'NAT', 'MSQ']:
            if i - 2 >= 0 and i + 1 < len(lines):
                q_num_str = lines[i-2]
                section = lines[i+1].upper()
                if q_num_str.isdigit() and section in ['GA', 'CS']:
                    q_num = int(q_num_str)
                    answer_parts = []
                    found_marks = False
                    for j in range(i+2, min(i+10, len(lines))):
                        if lines[j] in ['1', '2', '1.0', '2.0']:
                            found_marks = True
                            break
                        answer_parts.append(lines[j])
                    if found_marks:
                        answer = ' '.join(answer_parts).strip()
                        answer_map[f"{section}_{q_num}"] = normalize_answer(answer)

    # 2. If we got nothing, fallback to horizontal line-by-line regexes
    if len(answer_map) < 5:
        answer_map = {}
        current_section = "GA"
        for line in lines:
            trimmed = line.strip()
            if not trimmed:
                continue
            if GA_HEADER.search(trimmed):
                current_section = "GA"
                continue
            if CS_HEADER.search(trimmed):
                current_section = "CS"
                continue

            m = TABLE_ROW.match(trimmed)
            if m:
                q_num = m.group(1)
                section = m.group(3).upper()
                answer = normalize_answer(m.group(4))
                answer_map[f"{section}_{q_num}"] = answer
                continue

            m = PREFIXED.match(trimmed)
            if m:
                section = m.group(1).upper()
                q_num = m.group(2)
                answer = normalize_answer(m.group(3))
                answer_map[f"{section}_{q_num}"] = answer
                continue

            m = SIMPLE.match(trimmed)
            if m:
                q_num = m.group(1)
                # Avoid false positives on decimal ranges
                if not ("to" in m.group(2).lower() and "." in q_num):
                    answer = normalize_answer(m.group(2))
                    answer_map[f"{current_section}_{q_num}"] = answer
                    continue

    logger.info(f"Parsed {len(answer_map)} answers from answer key text.")
    return answer_map
