ALTER TABLE attempt_answers ADD COLUMN ease_factor DOUBLE PRECISION NOT NULL DEFAULT 2.5;
ALTER TABLE attempt_answers ADD COLUMN interval_days INTEGER NOT NULL DEFAULT 1;
ALTER TABLE attempt_answers ADD COLUMN next_review DATE;
ALTER TABLE attempt_answers ADD COLUMN repetitions INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_attempt_answers_next_review ON attempt_answers(next_review) WHERE next_review IS NOT NULL;
