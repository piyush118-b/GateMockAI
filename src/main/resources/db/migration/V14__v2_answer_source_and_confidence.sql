-- ==========================================
-- V14: GateMockAI v2.1 — Answer Source & Confidence Schema
-- Removes answer-key dependency; Gemini now derives answers directly.
-- ==========================================

-- 1. Add v2.1 columns to questions table
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS answer_source    VARCHAR(50)   NOT NULL DEFAULT 'HUMAN_VERIFIED',
    ADD COLUMN IF NOT EXISTS confidence_score NUMERIC(3,2),
    ADD COLUMN IF NOT EXISTS review_status    VARCHAR(50)   NOT NULL DEFAULT 'PUBLISHED';

-- 2. Tag all existing rows: they came from real answer key PDFs
UPDATE questions
    SET answer_source = 'HUMAN_VERIFIED',
        review_status = 'PUBLISHED'
    WHERE answer_source = 'HUMAN_VERIFIED';

-- 3. Admin review queue — holds low-confidence LLM answers for human review
CREATE TABLE IF NOT EXISTS admin_review_queue (
    id               BIGSERIAL    PRIMARY KEY,
    question_id      VARCHAR(100) NOT NULL REFERENCES questions(question_id) ON DELETE CASCADE,
    flagged_reason   TEXT,
    confidence_score NUMERIC(3,2),
    flagged_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by      VARCHAR(150),
    reviewed_at      TIMESTAMP,
    resolved         BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (question_id)
);

CREATE INDEX IF NOT EXISTS idx_review_queue_resolved
    ON admin_review_queue (resolved, confidence_score ASC, flagged_at ASC);
