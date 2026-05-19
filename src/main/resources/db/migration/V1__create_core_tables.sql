-- USERS
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' CHECK (role IN ('ADMIN','STUDENT')),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- MOCK TESTS
CREATE TABLE mock_tests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    duration_minutes INT NOT NULL DEFAULT 180,
    total_marks NUMERIC(6,2),
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- QUESTIONS
CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL REFERENCES mock_tests(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('MCQ','MSQ','NAT')),
    correct_nat_value FLOAT,
    nat_tolerance FLOAT DEFAULT 0,
    marks NUMERIC(4,2) NOT NULL DEFAULT 1,
    negative_marks NUMERIC(4,2) NOT NULL DEFAULT 0,
    sequence_no INT NOT NULL,
    explanation TEXT
);

-- OPTIONS (for MCQ and MSQ only)
CREATE TABLE options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    option_label CHAR(1) NOT NULL,     -- A, B, C, D
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false
);

-- ATTEMPTS (one per student per test session)
CREATE TABLE attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    test_id UUID NOT NULL REFERENCES mock_tests(id),
    score NUMERIC(6,2),
    started_at TIMESTAMP NOT NULL DEFAULT now(),
    submitted_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS','SUBMITTED','TIMED_OUT'))
);

-- ATTEMPT ANSWERS (one row per question answered)
CREATE TABLE attempt_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id),
    selected_option_ids TEXT,          -- comma-separated UUIDs for MCQ/MSQ
    nat_value_entered FLOAT,
    is_correct BOOLEAN,
    marks_awarded NUMERIC(4,2)
);
