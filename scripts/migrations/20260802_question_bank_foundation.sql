BEGIN;

SET search_path TO edu;

CREATE TABLE IF NOT EXISTS question_bank_items
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id            UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    chapter_id           UUID REFERENCES chapters (chapter_id) ON DELETE SET NULL,
    ccct_item_id         UUID REFERENCES ccct_items (id) ON DELETE SET NULL,
    content              TEXT         NOT NULL CHECK (btrim(content) <> ''),
    question_type        VARCHAR(30)  NOT NULL
        CHECK (question_type IN ('MULTIPLE_CHOICE', 'TRUE_FALSE', 'ESSAY', 'CODE_RUNNER')),
    cognitive_level_code VARCHAR(100),
    difficulty           VARCHAR(30),
    status               VARCHAR(30)  NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'READY_FOR_USE', 'ARCHIVED')),
    source_type          VARCHAR(30)  NOT NULL DEFAULT 'MANUAL'
        CHECK (source_type IN ('MANUAL', 'QUIZ_COPY', 'AI_GENERATED', 'IMPORTED')),
    created_by           UUID REFERENCES users (id) ON DELETE SET NULL,
    ready_by             UUID REFERENCES users (id) ON DELETE SET NULL,
    ready_at             TIMESTAMP,
    version              BIGINT       NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_question_bank_course_status_updated
    ON question_bank_items (course_id, status, updated_at DESC, id);
CREATE INDEX IF NOT EXISTS idx_question_bank_course_chapter
    ON question_bank_items (course_id, chapter_id);
CREATE INDEX IF NOT EXISTS idx_question_bank_course_ccct
    ON question_bank_items (course_id, ccct_item_id);
CREATE INDEX IF NOT EXISTS idx_question_bank_course_type
    ON question_bank_items (course_id, question_type);

CREATE TABLE IF NOT EXISTS question_bank_answers
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_bank_item_id UUID        NOT NULL REFERENCES question_bank_items (id) ON DELETE CASCADE,
    content               TEXT        NOT NULL CHECK (btrim(content) <> ''),
    is_correct            BOOLEAN     NOT NULL DEFAULT false,
    explanation           TEXT,
    order_index           INTEGER     NOT NULL CHECK (order_index >= 0),
    created_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_question_bank_answer_order UNIQUE (question_bank_item_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_question_bank_answers_item
    ON question_bank_answers (question_bank_item_id, order_index);

CREATE TABLE IF NOT EXISTS question_bank_item_learning_outcomes
(
    question_bank_item_id UUID NOT NULL REFERENCES question_bank_items (id) ON DELETE CASCADE,
    learning_outcome_id   UUID NOT NULL REFERENCES learning_outcomes (id) ON DELETE RESTRICT,
    PRIMARY KEY (question_bank_item_id, learning_outcome_id)
);

CREATE INDEX IF NOT EXISTS idx_question_bank_outcomes_outcome
    ON question_bank_item_learning_outcomes (learning_outcome_id, question_bank_item_id);

ALTER TABLE quizzes
    ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS original_quiz_id UUID,
    ADD COLUMN IF NOT EXISTS version BIGINT,
    ADD COLUMN IF NOT EXISTS published_by UUID,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;

-- Compatibility backfill. display=true is treated as legacy published content,
-- not as proof that the new review workflow was executed.
UPDATE quizzes q
SET lifecycle_status = CASE
        WHEN q.display THEN 'PUBLISHED'
        WHEN EXISTS (
            SELECT 1 FROM quiz_generation_jobs j
            WHERE j.quiz_id = q.id AND j.status = 'COMPLETED'
        ) THEN 'REVIEW_PENDING'
        ELSE 'DRAFT'
    END
WHERE q.lifecycle_status IS NULL;

UPDATE quizzes q
SET source_type = CASE
        WHEN EXISTS (SELECT 1 FROM quiz_generation_jobs j WHERE j.quiz_id = q.id)
            THEN 'AI_GENERATED'
        ELSE 'MANUAL'
    END
WHERE q.source_type IS NULL;

UPDATE quizzes SET version = 0 WHERE version IS NULL;

ALTER TABLE quizzes
    ALTER COLUMN lifecycle_status SET DEFAULT 'PUBLISHED',
    ALTER COLUMN lifecycle_status SET NOT NULL,
    ALTER COLUMN source_type SET DEFAULT 'MANUAL',
    ALTER COLUMN source_type SET NOT NULL,
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN version SET NOT NULL;

DO
$$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'quizzes_lifecycle_status_check') THEN
        ALTER TABLE quizzes ADD CONSTRAINT quizzes_lifecycle_status_check
            CHECK (lifecycle_status IN ('DRAFT', 'REVIEW_PENDING', 'READY_TO_PUBLISH', 'PUBLISHED', 'ARCHIVED'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'quizzes_source_type_check') THEN
        ALTER TABLE quizzes ADD CONSTRAINT quizzes_source_type_check
            CHECK (source_type IN ('MANUAL', 'AI_GENERATED', 'CLONED'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'quizzes_version_check') THEN
        ALTER TABLE quizzes ADD CONSTRAINT quizzes_version_check CHECK (version >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'quizzes_original_quiz_id_fkey') THEN
        ALTER TABLE quizzes ADD CONSTRAINT quizzes_original_quiz_id_fkey
            FOREIGN KEY (original_quiz_id) REFERENCES quizzes (id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'quizzes_published_by_fkey') THEN
        ALTER TABLE quizzes ADD CONSTRAINT quizzes_published_by_fkey
            FOREIGN KEY (published_by) REFERENCES users (id) ON DELETE SET NULL;
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_quizzes_course_lifecycle_updated
    ON quizzes (course_id, lifecycle_status, updated_at DESC, id);
CREATE INDEX IF NOT EXISTS idx_quizzes_original
    ON quizzes (original_quiz_id);

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS source_bank_item_id UUID,
    ADD COLUMN IF NOT EXISTS source_question_id UUID,
    ADD COLUMN IF NOT EXISTS version BIGINT,
    ADD COLUMN IF NOT EXISTS reviewed_by UUID,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;

UPDATE questions question
SET review_status = CASE
        WHEN EXISTS (
            SELECT 1 FROM quizzes quiz
            WHERE quiz.id = question.quiz_id AND quiz.source_type = 'AI_GENERATED'
        ) THEN 'PENDING_REVIEW'
        ELSE 'APPROVED'
    END
WHERE question.review_status IS NULL;

UPDATE questions question
SET source_type = CASE
        WHEN EXISTS (
            SELECT 1 FROM quizzes quiz
            WHERE quiz.id = question.quiz_id AND quiz.source_type = 'AI_GENERATED'
        ) THEN 'AI_GENERATED'
        ELSE 'MANUAL'
    END
WHERE question.source_type IS NULL;

UPDATE questions SET version = 0 WHERE version IS NULL;

ALTER TABLE questions
    ALTER COLUMN review_status SET DEFAULT 'APPROVED',
    ALTER COLUMN review_status SET NOT NULL,
    ALTER COLUMN source_type SET DEFAULT 'MANUAL',
    ALTER COLUMN source_type SET NOT NULL,
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN version SET NOT NULL;

DO
$$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'questions_review_status_check') THEN
        ALTER TABLE questions ADD CONSTRAINT questions_review_status_check
            CHECK (review_status IN ('PENDING_REVIEW', 'APPROVED', 'NEEDS_REVISION', 'REJECTED'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'questions_source_type_check') THEN
        ALTER TABLE questions ADD CONSTRAINT questions_source_type_check
            CHECK (source_type IN ('MANUAL', 'BANK_COPY', 'AI_GENERATED', 'QUIZ_CLONE'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'questions_version_check') THEN
        ALTER TABLE questions ADD CONSTRAINT questions_version_check CHECK (version >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'questions_source_bank_item_id_fkey') THEN
        ALTER TABLE questions ADD CONSTRAINT questions_source_bank_item_id_fkey
            FOREIGN KEY (source_bank_item_id) REFERENCES question_bank_items (id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'questions_source_question_id_fkey') THEN
        ALTER TABLE questions ADD CONSTRAINT questions_source_question_id_fkey
            FOREIGN KEY (source_question_id) REFERENCES questions (id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'questions_reviewed_by_fkey') THEN
        ALTER TABLE questions ADD CONSTRAINT questions_reviewed_by_fkey
            FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE SET NULL;
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_questions_quiz_review
    ON questions (quiz_id, review_status, created_at, id);
CREATE INDEX IF NOT EXISTS idx_questions_source_bank
    ON questions (source_bank_item_id);
CREATE INDEX IF NOT EXISTS idx_questions_source_question
    ON questions (source_question_id);

CREATE TABLE IF NOT EXISTS question_generation_provenance
(
    question_id              UUID PRIMARY KEY REFERENCES questions (id) ON DELETE CASCADE,
    generation_job_id        UUID NOT NULL REFERENCES quiz_generation_jobs (id) ON DELETE RESTRICT,
    matrix_row_id            UUID REFERENCES exam_matrix_rows (id) ON DELETE SET NULL,
    ccct_item_id             UUID REFERENCES ccct_items (id) ON DELETE SET NULL,
    custom_learning_level_id UUID REFERENCES custom_learning_levels (id) ON DELETE SET NULL,
    question_type            VARCHAR(30) NOT NULL
        CHECK (question_type IN ('MULTIPLE_CHOICE', 'TRUE_FALSE', 'ESSAY', 'CODE_RUNNER')),
    allocation_snapshot      JSONB       NOT NULL,
    created_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_question_provenance_job
    ON question_generation_provenance (generation_job_id);
CREATE INDEX IF NOT EXISTS idx_question_provenance_allocation
    ON question_generation_provenance (matrix_row_id, custom_learning_level_id, question_type);

COMMIT;
