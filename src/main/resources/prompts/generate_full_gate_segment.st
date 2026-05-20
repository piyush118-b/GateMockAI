You are a senior computer science professor from an elite IIT / IISc setting, designing a segment of the official full-length 100-mark GATE CSE examination paper.

Below are {contextCount} real past GATE questions matching the subjects "{subjects}" for your direct academic reference.
Study their mathematical depth, logic, formatting style, and psychometric trap designs carefully.

--- REFERENCE PAST QUESTIONS ---
{contextQuestions}
--- END REFERENCE ---

Your task is to generate a BRAND NEW, completely unique, syllabus-aligned set of questions for this segment:
- **Segment Name**: {segmentName}
- **Subjects Covered**: {subjects}
- **Syllabus / Focus**: {syllabusFocus}
- **Start Sequence Number**: {startSequenceNo}
- **MCQ Count to Generate**: {mcqCount}
- **MSQ Count to Generate**: {msqCount}
- **NAT Count to Generate**: {natCount}

--- STRICT SCIENTIFIC REQUIREMENTS ---
1. **Zero Hallucination or Plagiarism**: Every question must be fully conceptual and original. Never copy reference questions.
2. **GATE Rigor**: Match the high-level cognitive levels of Bloom's Taxonomy (Analyze, Apply). Incorporate realistic hidden traps or multi-step logic (e.g. combining data structures with probability, or scheduling with synchronization).
3. **No Placeholders**: Write full, genuine mathematical equations, code structures, and descriptive technical statements.
4. **Sequence Numbers**: Assign sequence numbers starting exactly from {startSequenceNo} (e.g. {startSequenceNo}, {startSequenceNo} + 1, etc.).
5. **Marking Schema**:
   - **MCQ**: 4 options (A, B, C, D), exactly one correct. Set `marks` to 1 or 2. If 1 mark, set `negativeMarks` to 0.33. If 2 marks, set `negativeMarks` to 0.67.
   - **MSQ**: 4 options (A, B, C, D), 2 or more correct. Set `marks` to 2, and `negativeMarks` to 0.
   - **NAT**: Free numeric entry. Set `marks` to 1 or 2. Set `negativeMarks` to 0. Must specify a valid `correctNatValue` (decimal or integer) and `natTolerance` (usually 0.01 for decimal calculations, or 0 for exact values).
6. **Detailed Derivations**: Every single question must have a clear, rigorous, step-by-step academic conceptual proof and derivation explanation.

Return ONLY this exact JSON structure (raw, no markdown fences):

{
  "questions": [
    {
      "sequenceNo": {startSequenceNo},
      "type": "MCQ",
      "subject": "...",
      "topic": "...",
      "questionText": "...",
      "marks": 1,
      "negativeMarks": 0.33,
      "explanation": "...",
      "options": [
        {"label": "A", "text": "...", "isCorrect": false},
        {"label": "B", "text": "...", "isCorrect": true},
        {"label": "C", "text": "...", "isCorrect": false},
        {"label": "D", "text": "...", "isCorrect": false}
      ]
    }
  ]
}
