-- ============================================================
-- V5: Branch/Subject Registry, Image Attachments, Paper Config
-- ============================================================

-- BRANCH REGISTRY (CSE, EE, ME, etc.)
CREATE TABLE branches (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(20)  NOT NULL UNIQUE
);

-- SUBJECTS PER BRANCH (with default mark weightage)
CREATE TABLE branch_subjects (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id               UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    name                    VARCHAR(255) NOT NULL,
    default_marks_weightage INT NOT NULL DEFAULT 0,
    display_order           INT NOT NULL DEFAULT 0,
    is_active               BOOLEAN NOT NULL DEFAULT true
);

-- PAPER GENERATION CONFIG (saved weightage presets)
CREATE TABLE paper_configs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id        UUID NOT NULL REFERENCES branches(id),
    year_label       VARCHAR(20),
    total_marks      INT NOT NULL DEFAULT 100,
    duration_minutes INT NOT NULL DEFAULT 180,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE paper_config_subjects (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_id        UUID NOT NULL REFERENCES paper_configs(id) ON DELETE CASCADE,
    subject_id       UUID NOT NULL REFERENCES branch_subjects(id),
    allocated_marks  INT NOT NULL DEFAULT 0
);

-- MEDIA ATTACHMENTS (question images, option images)
CREATE TABLE media_attachments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,   -- 'QUESTION' or 'OPTION'
    entity_id   UUID NOT NULL,
    file_path   VARCHAR(500) NOT NULL,  -- e.g. /uploads/questions/abc.png
    mime_type   VARCHAR(100),
    uploaded_at TIMESTAMP NOT NULL DEFAULT now()
);

-- EXTEND mock_tests with branch + year context
ALTER TABLE mock_tests
    ADD COLUMN branch       VARCHAR(100),
    ADD COLUMN year_label   VARCHAR(20),
    ADD COLUMN config_id    UUID REFERENCES paper_configs(id);

-- EXTEND questions with image support
ALTER TABLE questions
    ADD COLUMN image_path TEXT;

-- EXTEND options with image support
ALTER TABLE options
    ADD COLUMN image_path TEXT;

-- ==============================================
-- SEED: CSE Branch and Standard GATE Subjects
-- ==============================================

INSERT INTO branches (id, name, code) VALUES
    ('a1b2c3d4-0001-0001-0001-000000000001', 'Computer Science & Engineering', 'CSE');

INSERT INTO branch_subjects (branch_id, name, default_marks_weightage, display_order) VALUES
    ('a1b2c3d4-0001-0001-0001-000000000001', 'General Aptitude',                          15, 1),
    ('a1b2c3d4-0001-0001-0001-000000000001', 'Engineering Mathematics',                   13, 2),
    ('a1b2c3d4-0001-0001-0001-000000000001', 'Programming & Data Structures',              8,  3),
    ('a1b2c3d4-0001-0001-0001-000000000001', 'Algorithms',                                8,  4),
    ('a1b2c3d4-0001-0001-0001-000000000001', 'Theory of Computation',                     7,  5),
    ('a1b2c3d4-0001-0001-0001-000000000001', 'Compiler Design',                           5,  6),
    ('a1b2c3d4-0001-0001-0001-000000000001', 'Operating Systems',                         8,  7),
    ('a1b2c3d4-0001-0001-0001-000000000001', 'Databases (DBMS)',                          8,  8),
    ('a1b2c3d4-0001-0001-0001-000000000001', 'Computer Networks',                         8,  9),
    ('a1b2c3d4-0001-0001-0001-000000000001', 'Computer Organization & Architecture',      8,  10),
    ('a1b2c3d4-0001-0001-0001-000000000001', 'Digital Logic',                             5,  11),
    ('a1b2c3d4-0001-0001-0001-000000000001', 'Software Engineering',                      7,  12);
