CREATE TABLE explanation_cache (
    id             BIGSERIAL PRIMARY KEY,
    question_id    UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    student_answer TEXT   NOT NULL,
    explanation    TEXT   NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(question_id, student_answer)
);
