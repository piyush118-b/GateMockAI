-- ==========================================
-- V13: GATE Question Intelligence Platform Schema
-- Drops all old mock-test tables and creates the
-- production-grade AI-Decoupled pipeline schema.
-- ==========================================

-- -------------------------------------------------------
-- STEP 1: Drop all old tables in reverse dependency order
-- -------------------------------------------------------

DROP TABLE IF EXISTS attempt_answers          CASCADE;
DROP TABLE IF EXISTS attempts                 CASCADE;
DROP TABLE IF EXISTS spaced_repetition_items  CASCADE;
DROP TABLE IF EXISTS explanation_cache        CASCADE;
DROP TABLE IF EXISTS media_attachments        CASCADE;
DROP TABLE IF EXISTS paper_config_subjects    CASCADE;
DROP TABLE IF EXISTS paper_configs            CASCADE;
DROP TABLE IF EXISTS branch_subjects          CASCADE;
DROP TABLE IF EXISTS branches                 CASCADE;
DROP TABLE IF EXISTS options                  CASCADE;
DROP TABLE IF EXISTS questions                CASCADE;
DROP TABLE IF EXISTS mock_tests               CASCADE;

-- -------------------------------------------------------
-- STEP 2: Enable pgvector (idempotent)
-- -------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS vector;

-- -------------------------------------------------------
-- STEP 3: Create new production-grade schema
-- -------------------------------------------------------

-- 1. Papers Table (GATE paper registry)
CREATE TABLE papers (
    paper_id        VARCHAR(100) PRIMARY KEY,
    year            INT          NOT NULL,
    exam_name       VARCHAR(100) NOT NULL,
    branch          VARCHAR(100),
    session         VARCHAR(50),
    duration        INT,
    total_marks     NUMERIC(5,2),
    total_questions INT,
    paper_type      VARCHAR(50)  NOT NULL DEFAULT 'Official',
    status          VARCHAR(50)  NOT NULL DEFAULT 'Extracted',
    uploaded_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Questions Table (Source of Truth — extracted only, immutable after write)
CREATE TABLE questions (
    question_id     VARCHAR(100) PRIMARY KEY,
    paper_id        VARCHAR(100) NOT NULL REFERENCES papers(paper_id) ON DELETE CASCADE,
    question_number INT          NOT NULL,
    section         VARCHAR(100),
    question_type   VARCHAR(50),              -- MCQ, MSQ, NAT
    marks           NUMERIC(4,2),
    negative_marks  NUMERIC(4,2) NOT NULL DEFAULT 0.00,
    question_text   TEXT         NOT NULL,
    correct_answer  TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (paper_id, question_number)
);

-- 3. Options Table
CREATE TABLE options (
    option_id     VARCHAR(100) PRIMARY KEY,
    question_id   VARCHAR(100) NOT NULL REFERENCES questions(question_id) ON DELETE CASCADE,
    label         CHAR(1)      NOT NULL,       -- A, B, C, D
    option_text   TEXT         NOT NULL,
    display_order INT          NOT NULL
);

-- 4. Assets Table (MinIO references — NO binary data stored in DB)
CREATE TABLE assets (
    asset_id    VARCHAR(100) PRIMARY KEY,
    question_id VARCHAR(100) NOT NULL REFERENCES questions(question_id) ON DELETE CASCADE,
    option_id   VARCHAR(100) REFERENCES options(option_id) ON DELETE SET NULL,
    asset_type  VARCHAR(50)  NOT NULL,         -- Image, Graph, Table, Circuit, Tree, Automata, Flowchart
    bucket_name VARCHAR(100) NOT NULL,
    object_key  VARCHAR(500) NOT NULL,
    mime_type   VARCHAR(100),
    width       INT,
    height      INT,
    checksum    VARCHAR(64),
    uploaded_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Concepts Master Table
CREATE TABLE concepts (
    concept_id     VARCHAR(100) PRIMARY KEY,
    name           VARCHAR(150) NOT NULL UNIQUE,
    parent_concept VARCHAR(100) REFERENCES concepts(concept_id) ON DELETE SET NULL
);

-- 6. Question–Concept Many-to-Many
CREATE TABLE question_concepts (
    question_id VARCHAR(100) NOT NULL REFERENCES questions(question_id) ON DELETE CASCADE,
    concept_id  VARCHAR(100) NOT NULL REFERENCES concepts(concept_id)  ON DELETE CASCADE,
    importance  NUMERIC(3,2) NOT NULL CHECK (importance >= 0.00 AND importance <= 1.00),
    PRIMARY KEY (question_id, concept_id)
);

-- 7. Review History (manual QA tracking)
CREATE TABLE review_history (
    review_id   VARCHAR(100) PRIMARY KEY,
    question_id VARCHAR(100) NOT NULL REFERENCES questions(question_id) ON DELETE CASCADE,
    reviewer    VARCHAR(100) NOT NULL,
    status      VARCHAR(50)  NOT NULL,         -- PASSED, FAILED, NEEDS_REVIEW
    comments    TEXT,
    reviewed_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. AI Generation Log (audit trail for every LLM call)
CREATE TABLE ai_generation_log (
    log_id         VARCHAR(100) PRIMARY KEY,
    question_id    VARCHAR(100) NOT NULL REFERENCES questions(question_id) ON DELETE CASCADE,
    task           VARCHAR(100) NOT NULL,       -- MetadataGeneration, ExplanationGeneration, etc.
    model          VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50),
    status         VARCHAR(50)  NOT NULL,       -- SUCCESS, FAILED, SKIPPED
    token_usage    INT          NOT NULL DEFAULT 0,
    generated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------
-- AI ARTIFACT SYSTEM (versioned, extensible payloads)
-- Extraction data is immutable. AI artifacts are versioned.
-- -------------------------------------------------------

-- 9. Base AI Artifact Table
CREATE TABLE ai_artifacts (
    artifact_id   VARCHAR(100) PRIMARY KEY,
    question_id   VARCHAR(100) NOT NULL REFERENCES questions(question_id) ON DELETE CASCADE,
    artifact_type VARCHAR(50)  NOT NULL DEFAULT 'PENDING',  -- METADATA, EXPLANATION, EMBEDDING, HINT, CONCEPT
    version       INT          NOT NULL DEFAULT 1,
    model         VARCHAR(100) NOT NULL,
    status        VARCHAR(50)  NOT NULL DEFAULT 'PENDING',  -- PENDING, GENERATED, VERIFIED, ERROR
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. AI Metadata Details Sub-table (Worker 1 output)
CREATE TABLE ai_metadata_details (
    artifact_id    VARCHAR(100) PRIMARY KEY REFERENCES ai_artifacts(artifact_id) ON DELETE CASCADE,
    subject        VARCHAR(150) NOT NULL,
    topic          VARCHAR(150) NOT NULL,
    subtopic       VARCHAR(150),
    difficulty     VARCHAR(50),               -- EASY, MEDIUM, HARD
    blooms_level   VARCHAR(50),               -- Remember, Understand, Apply, Analyze, Evaluate, Create
    estimated_time INT,                       -- in seconds
    confidence     NUMERIC(3,2) CHECK (confidence >= 0.00 AND confidence <= 1.00)
);

-- 11. Explanation Details Sub-table (Worker 2 output)
CREATE TABLE explanation_details (
    artifact_id      VARCHAR(100) PRIMARY KEY REFERENCES ai_artifacts(artifact_id) ON DELETE CASCADE,
    explanation_text TEXT         NOT NULL,
    final_answer     TEXT,
    alt_approach     TEXT                     -- Alternative solution approach
);

-- 12. Embedding Details Sub-table (links to gate_vector_store row)
CREATE TABLE embedding_details (
    artifact_id       VARCHAR(100) PRIMARY KEY REFERENCES ai_artifacts(artifact_id) ON DELETE CASCADE,
    provider          VARCHAR(100) NOT NULL,   -- e.g., gemini, ollama
    vector_db_id      VARCHAR(100) NOT NULL,   -- row ID in gate_vector_store
    embedding_version VARCHAR(50)
);

-- 13. Hint Details Sub-table (Worker 4 output)
CREATE TABLE hint_details (
    artifact_id      VARCHAR(100) PRIMARY KEY REFERENCES ai_artifacts(artifact_id) ON DELETE CASCADE,
    hint_text        TEXT         NOT NULL,
    complexity_level INT          NOT NULL DEFAULT 1   -- 1=easy hint, 2=medium, 3=near-answer
);

-- -------------------------------------------------------
-- STEP 4: Performance Indexes
-- -------------------------------------------------------

CREATE INDEX idx_questions_paper_id    ON questions(paper_id);
CREATE INDEX idx_questions_type        ON questions(question_type);
CREATE INDEX idx_options_question_id   ON options(question_id);
CREATE INDEX idx_assets_question_id    ON assets(question_id);
CREATE INDEX idx_ai_artifacts_question ON ai_artifacts(question_id);
CREATE INDEX idx_ai_artifacts_type     ON ai_artifacts(artifact_type);
CREATE INDEX idx_ai_artifacts_status   ON ai_artifacts(status);
CREATE INDEX idx_ai_gen_log_question   ON ai_generation_log(question_id);
CREATE INDEX idx_papers_status         ON papers(status);
CREATE INDEX idx_papers_year           ON papers(year);
CREATE INDEX idx_review_history_q      ON review_history(question_id);
