You are creating a GATE mock test. Below are {contextCount} real past GATE questions on the topic "{topic}" for reference.
Study their style, difficulty, mathematical depth, and format carefully.

--- REFERENCE QUESTIONS ---
{contextQuestions}
--- END REFERENCE ---

Now generate a BRAND NEW mock test on the topic: "{topic}" (Subject: {subject})

Requirements:
- Generate exactly {mcqCount} MCQ questions, {msqCount} MSQ questions, {natCount} NAT questions
- Every question must be UNIQUE and completely different from the reference questions above
- Match the mathematical rigor and difficulty level of the references exactly
- MCQ: exactly 4 options (A, B, C, D), exactly one correct. Apply -1/3 negative marking.
- MSQ: exactly 4 options, 2 or more correct. Zero negative marking. Award 2 marks.
- NAT: numerical answer, specify exact value and tolerance (usually 0 for integer answers, ±0.01 for decimals). Award 1 mark.
- Include a brief explanation for each question.

Return ONLY this exact JSON structure (raw, no markdown fences):

{
  "title": "GATE Mock Test: {topic}",
  "topic": "{topic}",
  "subject": "{subject}",
  "durationMinutes": 180,
  "questions": [
    {
      "sequenceNo": 1,
      "type": "MCQ",
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
    },
    {
      "sequenceNo": 2,
      "type": "MSQ",
      "questionText": "...",
      "marks": 2,
      "negativeMarks": 0,
      "explanation": "...",
      "options": [
        {"label": "A", "text": "...", "isCorrect": true},
        {"label": "B", "text": "...", "isCorrect": false},
        {"label": "C", "text": "...", "isCorrect": true},
        {"label": "D", "text": "...", "isCorrect": false}
      ]
    },
    {
      "sequenceNo": 3,
      "type": "NAT",
      "questionText": "...",
      "marks": 1,
      "negativeMarks": 0,
      "correctNatValue": 42,
      "natTolerance": 0,
      "explanation": "..."
    }
  ]
}
