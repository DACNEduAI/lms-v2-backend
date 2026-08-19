CREATE SCHEMA IF NOT EXISTS edu;

-- Set the default schema for the rest of this script!
SET search_path TO edu;

-- Cloud vector-index registry deliberately lives outside the resettable LMS
-- schema. initDB may rebuild edu.* but must retain completed Qdrant indexes.
CREATE SCHEMA IF NOT EXISTS ai_index;
CREATE TABLE IF NOT EXISTS ai_index.document_indexes
(
    bucket_name     VARCHAR(255) NOT NULL,
    file_key        VARCHAR(1024) NOT NULL,
    index_profile   VARCHAR(255) NOT NULL,
    source_hash     VARCHAR(64) NOT NULL,
    collection_name VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('READY', 'FAILED')),
    chunk_count     INTEGER NOT NULL DEFAULT 0,
    error_message   TEXT,
    indexed_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (bucket_name, file_key, index_profile)
);
CREATE INDEX IF NOT EXISTS idx_document_indexes_ready
    ON ai_index.document_indexes (status, index_profile);



CREATE TABLE IF NOT EXISTS users
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name  VARCHAR(255),
    avatar_url VARCHAR(255),
    role       VARCHAR(50),
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS students
(
-- Define the column, make it the Primary Key, and reference the users table
    user_id         UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    student_id      VARCHAR(255) NOT NULL UNIQUE,
    enrollment_year BIGINT       NOT NULL

);

CREATE TABLE IF NOT EXISTS teachers
(
-- Define the column, make it the Primary Key, and reference the users table
    user_id        UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    employee_id    VARCHAR(255) NOT NULL UNIQUE,
    phone_number   VARCHAR(255),
    department     VARCHAR(255),
    specialization VARCHAR(255)
);

-- Courses table
CREATE TABLE IF NOT EXISTS courses
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    course_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    course_code VARCHAR(255) NOT NULL UNIQUE,
    active      BOOLEAN      NOT NULL DEFAULT true,
    cover_image VARCHAR(255),
    start_date  TIMESTAMP    NOT NULL,
    end_date    TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

-- Chapters table
CREATE TABLE IF NOT EXISTS chapters
(
    chapter_id  UUID      DEFAULT gen_random_uuid() UNIQUE,
    title       VARCHAR(255),
    description TEXT,
    course_id   UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chapter_id)
);

-- Materials table
CREATE TABLE IF NOT EXISTS materials
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(255),
    type            VARCHAR(50) CHECK (type IN ('PDF', 'VIDEO', 'IMAGE')),
    file_key        VARCHAR(255) UNIQUE,
    is_downloadable BOOLEAN          DEFAULT false,
    status          VARCHAR(50)      DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'UPLOADED', 'FAILED')),
    ingestion_status VARCHAR(50)     NOT NULL DEFAULT 'NOT_STARTED' CHECK (ingestion_status IN ('NOT_STARTED', 'PENDING', 'PROCESSING', 'READY', 'FAILED', 'NOT_APPLICABLE')),
    ingestion_error TEXT,
    ingestion_revision UUID,
    ingested_at     TIMESTAMP,
    course_id       UUID,
    created_at      TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

-- Keep existing databases compatible when schema.sql runs against tables that
-- predate material indexing.
ALTER TABLE materials ADD COLUMN IF NOT EXISTS ingestion_status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED';
ALTER TABLE materials ADD COLUMN IF NOT EXISTS ingestion_error TEXT;
ALTER TABLE materials ADD COLUMN IF NOT EXISTS ingestion_revision UUID;
ALTER TABLE materials ADD COLUMN IF NOT EXISTS ingested_at TIMESTAMP;
UPDATE materials
SET ingestion_status = CASE
    WHEN type = 'PDF' AND status = 'UPLOADED' THEN 'PENDING'
    WHEN type <> 'PDF' AND status = 'UPLOADED' THEN 'NOT_APPLICABLE'
    ELSE ingestion_status
END
WHERE ingestion_status = 'NOT_STARTED';

CREATE TABLE IF NOT EXISTS material_index_jobs
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id       UUID        NOT NULL,
    material_id     UUID        NOT NULL,
    operation       VARCHAR(20) NOT NULL CHECK (operation IN ('INGEST', 'DELETE')),
    revision        UUID,
    file_key        VARCHAR(255),
    status          VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    attempt_count   INTEGER     NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    lease_until     TIMESTAMPTZ,
    error_message   TEXT,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_material_index_queue
    ON material_index_jobs (status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_material_index_lease
    ON material_index_jobs (status, lease_until);
CREATE INDEX IF NOT EXISTS idx_material_index_identity
    ON material_index_jobs (course_id, material_id, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_material_index_processing
    ON material_index_jobs (course_id, material_id)
    WHERE status = 'PROCESSING';
CREATE UNIQUE INDEX IF NOT EXISTS uq_material_index_active_ingest_revision
    ON material_index_jobs (course_id, material_id, revision)
    WHERE operation = 'INGEST' AND status IN ('PENDING', 'PROCESSING');

UPDATE materials
SET ingestion_revision = gen_random_uuid()
WHERE ingestion_status = 'PENDING' AND ingestion_revision IS NULL;
INSERT INTO material_index_jobs
    (course_id, material_id, operation, revision, file_key, status, next_attempt_at)
SELECT m.course_id, m.id, 'INGEST', m.ingestion_revision, m.file_key, 'PENDING', NOW()
FROM materials m
WHERE m.status = 'UPLOADED' AND m.type = 'PDF' AND m.ingestion_status = 'PENDING'
  AND NOT EXISTS (
      SELECT 1 FROM material_index_jobs j
      WHERE j.material_id = m.id AND j.revision = m.ingestion_revision
        AND j.status IN ('PENDING', 'PROCESSING', 'COMPLETED')
  );

CREATE TABLE IF NOT EXISTS chapter_materials
(
    chapter_id  UUID NOT NULL REFERENCES chapters (chapter_id) ON DELETE CASCADE,

    -- Reference to material table
    material_id UUID NOT NULL REFERENCES materials (id) ON DELETE CASCADE,

    PRIMARY KEY (material_id, chapter_id)
);


-- Group Learning Outcomes table (weak entity dependent on Course)
CREATE TABLE IF NOT EXISTS group_learning_outcomes
(
    id          UUID UNIQUE DEFAULT gen_random_uuid(),
    code        VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order  INTEGER      NOT NULL DEFAULT 1,
    course_id   UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, course_id)
);

-- Learning outcomes table (weak entity dependent on Course)
CREATE TABLE IF NOT EXISTS learning_outcomes
(
    id                        UUID UNIQUE DEFAULT gen_random_uuid(),
    prefix_number             INTEGER,
    description               TEXT,
    sort_order                INTEGER      NOT NULL DEFAULT 1,
    course_id                 UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    group_learning_outcome_id UUID,
    created_at                TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, course_id),
    FOREIGN KEY (group_learning_outcome_id, course_id) REFERENCES group_learning_outcomes (id, course_id) ON DELETE SET NULL (group_learning_outcome_id)
);

-- Junction table for many-to-many: chapters <-> learning_outcomes
CREATE TABLE IF NOT EXISTS chapter_learning_outcomes
(
    chapter_id          UUID NOT NULL REFERENCES chapters (chapter_id) ON DELETE CASCADE,
    learning_outcome_id UUID NOT NULL REFERENCES learning_outcomes (id) ON DELETE CASCADE,
    PRIMARY KEY (chapter_id, learning_outcome_id)
);

-- Course Content Coverage Template (CCCT)
CREATE TABLE IF NOT EXISTS ccct_profiles
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id        UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    name             VARCHAR(255) NOT NULL CHECK (btrim(name) <> ''),
    template_title   VARCHAR(255) NOT NULL CHECK (btrim(template_title) <> ''),
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    version          BIGINT       NOT NULL DEFAULT 0 CHECK (version >= 0),
    format_version   INTEGER      NOT NULL DEFAULT 2 CHECK (format_version IN (1, 2)),
    structured_current BOOLEAN    NOT NULL DEFAULT FALSE,
    last_mode        VARCHAR(20)  NOT NULL
        CHECK (last_mode IN ('PDF', 'CUSTOM', 'STRUCTURED', 'MARKDOWN')),
    markdown_content TEXT         NOT NULL DEFAULT '',
    created_by       UUID REFERENCES users (id) ON DELETE SET NULL,
    published_at     TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ccct_profile_name_per_course
    ON ccct_profiles (course_id, lower(btrim(name)));
CREATE UNIQUE INDEX IF NOT EXISTS uq_ccct_one_published_per_course
    ON ccct_profiles (course_id)
    WHERE status = 'PUBLISHED';
CREATE INDEX IF NOT EXISTS idx_ccct_profiles_course_updated
    ON ccct_profiles (course_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS ccct_chapters
(
    id                UUID PRIMARY KEY,
    profile_id        UUID         NOT NULL REFERENCES ccct_profiles (id) ON DELETE CASCADE,
    course_chapter_id UUID REFERENCES chapters (chapter_id) ON DELETE SET NULL,
    title             VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    custom_content    TEXT         NOT NULL DEFAULT '',
    order_index       INTEGER      NOT NULL CHECK (order_index >= 0),
    CONSTRAINT uq_ccct_chapter_order UNIQUE (profile_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_ccct_chapters_profile
    ON ccct_chapters (profile_id);
CREATE INDEX IF NOT EXISTS idx_ccct_chapters_course_chapter
    ON ccct_chapters (course_chapter_id);

-- Existing CCCT rows remain format v1 and are intentionally read-only. New
-- profiles use v2, which fixes their chapter set to the Course Chapters.
ALTER TABLE ccct_profiles ADD COLUMN IF NOT EXISTS format_version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE ccct_profiles ADD COLUMN IF NOT EXISTS structured_current BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ccct_profiles ALTER COLUMN format_version SET DEFAULT 2;
ALTER TABLE ccct_profiles DROP CONSTRAINT IF EXISTS ccct_profiles_last_mode_check;
ALTER TABLE ccct_profiles ADD CONSTRAINT ccct_profiles_last_mode_check
    CHECK (last_mode IN ('PDF', 'CUSTOM', 'STRUCTURED', 'MARKDOWN'));
ALTER TABLE ccct_chapters ADD COLUMN IF NOT EXISTS custom_content TEXT NOT NULL DEFAULT '';

CREATE TABLE IF NOT EXISTS ccct_ai_jobs
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id        UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    profile_id       UUID         NOT NULL REFERENCES ccct_profiles (id) ON DELETE CASCADE,
    type             VARCHAR(30)  NOT NULL CHECK (type IN ('PDF_TO_CUSTOM', 'CUSTOM_TO_STRUCTURED')),
    status           VARCHAR(20)  NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'STALE')),
    stage            VARCHAR(40)  NOT NULL DEFAULT 'QUEUED',
    expected_version BIGINT       NOT NULL CHECK (expected_version >= 0),
    file_key         VARCHAR(512),
    warnings_json    TEXT         NOT NULL DEFAULT '[]',
    error_message    TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ccct_ai_jobs_profile_created
    ON ccct_ai_jobs (profile_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ccct_ai_jobs_status_updated
    ON ccct_ai_jobs (status, updated_at);

CREATE TABLE IF NOT EXISTS ccct_topics
(
    id          UUID PRIMARY KEY,
    chapter_id  UUID         NOT NULL REFERENCES ccct_chapters (id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    order_index INTEGER      NOT NULL CHECK (order_index >= 0),
    CONSTRAINT uq_ccct_topic_order UNIQUE (chapter_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_ccct_topics_chapter
    ON ccct_topics (chapter_id);

CREATE TABLE IF NOT EXISTS ccct_items
(
    id              UUID PRIMARY KEY,
    topic_id        UUID        NOT NULL REFERENCES ccct_topics (id) ON DELETE CASCADE,
    content         TEXT        NOT NULL CHECK (btrim(content) <> ''),
    coverage_status VARCHAR(30) NOT NULL
        CHECK (coverage_status IN ('LEARN_AND_EXAM', 'LEARN_ONLY', 'SKIP')),
    note            TEXT        NOT NULL DEFAULT '',
    order_index     INTEGER     NOT NULL CHECK (order_index >= 0),
    CONSTRAINT uq_ccct_item_order UNIQUE (topic_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_ccct_items_topic_status
    ON ccct_items (topic_id, coverage_status);

-- Exam Matrices
-- Matrix 2 is deliberately independent from the existing Exam Matrix/Quiz2
-- tables. It is a teacher-authored planning surface and is not consumed by AI.
CREATE TABLE IF NOT EXISTS exam_matrices_2
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id   UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam_matrix_2_custom_learning_levels
(
    id          UUID PRIMARY KEY,
    matrix_id   UUID         NOT NULL REFERENCES exam_matrices_2 (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL CHECK (btrim(name) <> ''),
    sort_order  INTEGER      NOT NULL CHECK (sort_order >= 0)
);

CREATE TABLE IF NOT EXISTS exam_matrix_2_rows
(
    id                         UUID PRIMARY KEY,
    matrix_id                  UUID NOT NULL REFERENCES exam_matrices_2 (id) ON DELETE CASCADE,
    chapter_id                 UUID REFERENCES chapters (chapter_id) ON DELETE SET NULL,
    mapped_learning_outcome_id UUID REFERENCES learning_outcomes (id) ON DELETE SET NULL,
    sort_order                 INTEGER NOT NULL CHECK (sort_order >= 0)
);

CREATE TABLE IF NOT EXISTS exam_matrix_2_cell_counts
(
    row_id                     UUID NOT NULL REFERENCES exam_matrix_2_rows (id) ON DELETE CASCADE,
    custom_learning_level_id   UUID NOT NULL REFERENCES exam_matrix_2_custom_learning_levels (id) ON DELETE CASCADE,
    question_count             INTEGER NOT NULL DEFAULT 0 CHECK (question_count >= 0),
    PRIMARY KEY (row_id, custom_learning_level_id)
);

CREATE INDEX IF NOT EXISTS exam_matrices_2_course_idx ON exam_matrices_2 (course_id);
CREATE INDEX IF NOT EXISTS exam_matrix_2_rows_matrix_idx ON exam_matrix_2_rows (matrix_id, sort_order);

ALTER TABLE exam_matrices_2
    ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (lifecycle_status IN ('DRAFT', 'PUBLISHED')),
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    ADD COLUMN IF NOT EXISTS published_by UUID REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS exam_matrices
(
    id              uuid                        not null default gen_random_uuid(),
    course_id       uuid                        not null,
    title           character varying           not null,
    description     text                        null,
    created_at      timestamp without time zone null     default CURRENT_TIMESTAMP,
    updated_at      timestamp without time zone null     default CURRENT_TIMESTAMP,
    constraint exam_matrices_pkey primary key (id),
    constraint exam_matrices_course_id_fkey foreign KEY (course_id) references courses (id) on delete CASCADE
) TABLESPACE pg_default;

CREATE TABLE IF NOT EXISTS exam_matrix_rows
(
    id                  uuid              not null default gen_random_uuid(),
    matrix_id           uuid              not null,
    chapter_id          uuid              null,
    topic               character varying null,
    order_index         integer           not null,
    constraint exam_matrix_rows_pkey primary key (id),
    constraint exam_matrix_rows_id_matrix_key unique (id, matrix_id),
    constraint exam_matrix_rows_chapter_id_fkey foreign KEY (chapter_id) references chapters (chapter_id) on delete SET NULL,
    constraint exam_matrix_rows_matrix_id_fkey foreign KEY (matrix_id) references exam_matrices (id) on delete CASCADE
) TABLESPACE pg_default;

-- A matrix row can assess multiple learning outcomes. Learning outcomes can be
-- reused by rows in the same or different exam matrices.
CREATE TABLE IF NOT EXISTS exam_matrix_row_learning_outcomes
(
    exam_matrix_row_id UUID NOT NULL,
    learning_outcome_id UUID NOT NULL,
    is_selected        BOOLEAN NOT NULL DEFAULT FALSE,
    constraint exam_matrix_row_learning_outcomes_pkey primary key (exam_matrix_row_id, learning_outcome_id),
    constraint exam_matrix_row_learning_outcomes_row_id_fkey
        foreign key (exam_matrix_row_id) references exam_matrix_rows (id) on delete CASCADE,
    constraint exam_matrix_row_learning_outcomes_learning_outcome_id_fkey
        foreign key (learning_outcome_id) references learning_outcomes (id) on delete CASCADE
);

-- Custom learning levels belong to one exam matrix. Each level/outcome pair is
-- materialized; is_selected stores the teacher's checkbox state.
CREATE TABLE IF NOT EXISTS custom_learning_levels
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    matrix_id           UUID         NOT NULL,
    code                VARCHAR(255) NOT NULL CHECK (btrim(code) <> ''),
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    sort_order          INTEGER      NOT NULL DEFAULT 1,
    created_at          TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    constraint custom_learning_levels_matrix_id_fkey
        foreign key (matrix_id) references exam_matrices (id) on delete CASCADE,
    constraint custom_learning_levels_matrix_name_key unique (matrix_id, name),
    constraint custom_learning_levels_id_matrix_key unique (id, matrix_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS custom_learning_levels_matrix_code_key
    ON custom_learning_levels (matrix_id, lower(btrim(code)));

CREATE TABLE IF NOT EXISTS custom_learning_level_learning_outcomes
(
    custom_learning_level_id UUID NOT NULL,
    learning_outcome_id      UUID NOT NULL,
    is_selected              BOOLEAN NOT NULL DEFAULT FALSE,
    constraint custom_learning_level_learning_outcomes_pkey
        primary key (custom_learning_level_id, learning_outcome_id),
    constraint custom_learning_level_learning_outcomes_level_id_fkey
        foreign key (custom_learning_level_id) references custom_learning_levels (id) on delete CASCADE,
    constraint custom_learning_level_learning_outcomes_outcome_id_fkey
        foreign key (learning_outcome_id) references learning_outcomes (id) on delete CASCADE
);

-- Store one question count for each custom learning level used by a matrix row.
-- Counts whose row and level no longer share a selected outcome remain stored
-- as dormant configuration and are excluded from active totals/generation.
CREATE TABLE IF NOT EXISTS exam_matrix_row_learning_level_counts
(
    exam_matrix_row_id       UUID    NOT NULL,
    custom_learning_level_id UUID    NOT NULL,
    matrix_id                UUID    NOT NULL,
    question_type            VARCHAR(30),
    question_count           INTEGER NOT NULL DEFAULT 0 CHECK (question_count >= 0),
    constraint exam_matrix_row_learning_level_counts_question_type_check
        check (
            question_type is null or
            question_type in ('MULTIPLE_CHOICE', 'TRUE_FALSE', 'ESSAY', 'CODE_RUNNER')
        ),
    constraint exam_matrix_row_learning_level_counts_pkey
        primary key (exam_matrix_row_id, custom_learning_level_id),
    constraint exam_matrix_row_learning_level_counts_row_id_fkey
        foreign key (exam_matrix_row_id, matrix_id)
            references exam_matrix_rows (id, matrix_id) on delete CASCADE,
    constraint exam_matrix_row_learning_level_counts_level_id_fkey
        foreign key (custom_learning_level_id, matrix_id)
            references custom_learning_levels (id, matrix_id) on delete CASCADE
);

CREATE INDEX IF NOT EXISTS exam_matrix_row_learning_outcomes_outcome_idx
    ON exam_matrix_row_learning_outcomes (learning_outcome_id);
CREATE INDEX IF NOT EXISTS exam_matrix_row_learning_outcomes_selected_idx
    ON exam_matrix_row_learning_outcomes (exam_matrix_row_id, is_selected);
CREATE INDEX IF NOT EXISTS custom_learning_level_outcomes_outcome_idx
    ON custom_learning_level_learning_outcomes (learning_outcome_id);
CREATE INDEX IF NOT EXISTS custom_learning_level_outcomes_selected_idx
    ON custom_learning_level_learning_outcomes (custom_learning_level_id, is_selected);

-- Enrollments table
CREATE TABLE IF NOT EXISTS enrollments
(
    course_id     UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    enrolled_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (course_id, user_id)
);

-- Stable Bloom taxonomy. Exam Matrix custom levels remain matrix-owned and
-- are not referenced directly by reusable questions.
CREATE TABLE IF NOT EXISTS cognitive_levels
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code       VARCHAR(30)  NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    sort_order INTEGER      NOT NULL UNIQUE CHECK (sort_order > 0),
    CONSTRAINT cognitive_levels_code_check CHECK
        (code IN ('REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE', 'EVALUATE', 'CREATE'))
);

INSERT INTO cognitive_levels (code, name, sort_order) VALUES
    ('REMEMBER', 'Remember', 1),
    ('UNDERSTAND', 'Understand', 2),
    ('APPLY', 'Apply', 3),
    ('ANALYZE', 'Analyze', 4),
    ('EVALUATE', 'Evaluate', 5),
    ('CREATE', 'Create', 6)
ON CONFLICT (code) DO NOTHING;

-- Draft workspace. A Draft is editable and can never be selected by a Quiz.
CREATE TABLE IF NOT EXISTS question_drafts
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id          UUID        NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    chapter_id         UUID REFERENCES chapters (chapter_id) ON DELETE SET NULL,
    cognitive_level_id UUID REFERENCES cognitive_levels (id) ON DELETE RESTRICT,
    content            TEXT        NOT NULL CHECK (btrim(content) <> ''),
    content_format     VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT'
        CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    explanation       TEXT,
    explanation_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT'
        CHECK (explanation_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    question_type      VARCHAR(30) NOT NULL
        CHECK (question_type IN ('MULTIPLE_CHOICE', 'TRUE_FALSE', 'ESSAY', 'CODE_RUNNER')),
    code_runner_contract JSONB,
    source_type        VARCHAR(30) NOT NULL DEFAULT 'MANUAL'
        CHECK (source_type IN ('MANUAL', 'QUIZ_COPY', 'AI_GENERATED', 'IMPORTED')),
    created_by         UUID REFERENCES users (id) ON DELETE SET NULL,
    version            BIGINT      NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT question_drafts_code_runner_contract_check CHECK (
        (question_type = 'CODE_RUNNER' AND code_runner_contract IS NOT NULL)
        OR (question_type <> 'CODE_RUNNER' AND code_runner_contract IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_question_drafts_course_updated
    ON question_drafts (course_id, updated_at DESC, id);
CREATE INDEX IF NOT EXISTS idx_question_drafts_classification
    ON question_drafts (course_id, chapter_id, cognitive_level_id, question_type);

CREATE TABLE IF NOT EXISTS question_draft_answers
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_draft_id UUID      NOT NULL REFERENCES question_drafts (id) ON DELETE CASCADE,
    content           TEXT      NOT NULL CHECK (btrim(content) <> ''),
    is_correct        BOOLEAN   NOT NULL DEFAULT false,
    explanation       TEXT,
    content_format    VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT'
        CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    explanation_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT'
        CHECK (explanation_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    order_index       INTEGER   NOT NULL CHECK (order_index >= 0),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_question_draft_answer_order UNIQUE (question_draft_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_question_draft_answers_parent
    ON question_draft_answers (question_draft_id, order_index);

CREATE TABLE IF NOT EXISTS question_draft_learning_outcomes
(
    question_draft_id   UUID NOT NULL REFERENCES question_drafts (id) ON DELETE CASCADE,
    learning_outcome_id UUID NOT NULL REFERENCES learning_outcomes (id) ON DELETE RESTRICT,
    PRIMARY KEY (question_draft_id, learning_outcome_id)
);

CREATE INDEX IF NOT EXISTS idx_question_draft_outcomes_outcome
    ON question_draft_learning_outcomes (learning_outcome_id, question_draft_id);

-- Validated reusable questions. Quiz questions receive physical snapshots and
-- never read live content/answers from this table.
CREATE TABLE IF NOT EXISTS question_bank_questions
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    origin_draft_id    UUID        NOT NULL UNIQUE,
    course_id          UUID        NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    chapter_id         UUID REFERENCES chapters (chapter_id) ON DELETE SET NULL,
    cognitive_level_id UUID        NOT NULL REFERENCES cognitive_levels (id) ON DELETE RESTRICT,
    content            TEXT        NOT NULL CHECK (btrim(content) <> ''),
    content_format     VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT'
        CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    explanation       TEXT,
    explanation_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT'
        CHECK (explanation_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    question_type      VARCHAR(30) NOT NULL
        CHECK (question_type IN ('MULTIPLE_CHOICE', 'TRUE_FALSE', 'ESSAY', 'CODE_RUNNER')),
    code_runner_contract JSONB,
    source_type        VARCHAR(30) NOT NULL DEFAULT 'MANUAL'
        CHECK (source_type IN ('MANUAL', 'QUIZ_COPY', 'AI_GENERATED', 'IMPORTED')),
    created_by         UUID REFERENCES users (id) ON DELETE SET NULL,
    validated_by       UUID REFERENCES users (id) ON DELETE SET NULL,
    validated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version            BIGINT      NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT question_bank_questions_code_runner_contract_check CHECK (
        (question_type = 'CODE_RUNNER' AND code_runner_contract IS NOT NULL)
        OR (question_type <> 'CODE_RUNNER' AND code_runner_contract IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_question_bank_questions_course_updated
    ON question_bank_questions (course_id, updated_at DESC, id);
CREATE INDEX IF NOT EXISTS idx_question_bank_questions_classification
    ON question_bank_questions (course_id, chapter_id, cognitive_level_id, question_type);

CREATE TABLE IF NOT EXISTS question_bank_answers
(
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_bank_question_id UUID      NOT NULL REFERENCES question_bank_questions (id) ON DELETE CASCADE,
    content                   TEXT      NOT NULL CHECK (btrim(content) <> ''),
    is_correct                BOOLEAN   NOT NULL DEFAULT false,
    explanation               TEXT,
    content_format            VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT'
        CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    explanation_format        VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT'
        CHECK (explanation_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    order_index               INTEGER   NOT NULL CHECK (order_index >= 0),
    created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_question_bank_answer_order UNIQUE (question_bank_question_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_question_bank_answers_parent
    ON question_bank_answers (question_bank_question_id, order_index);

CREATE TABLE IF NOT EXISTS question_bank_question_learning_outcomes
(
    question_bank_question_id UUID NOT NULL REFERENCES question_bank_questions (id) ON DELETE CASCADE,
    learning_outcome_id       UUID NOT NULL REFERENCES learning_outcomes (id) ON DELETE RESTRICT,
    PRIMARY KEY (question_bank_question_id, learning_outcome_id)
);

CREATE INDEX IF NOT EXISTS idx_question_bank_question_outcomes_outcome
    ON question_bank_question_learning_outcomes (learning_outcome_id, question_bank_question_id);

-- Quizzes table
CREATE TABLE IF NOT EXISTS quizzes
(
    id                 UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    quiz_title         VARCHAR(255) NOT NULL,
    description        TEXT,
    unlimited          BOOLEAN      NOT NULL DEFAULT false,
    time_limit_minutes INTEGER,
    pass_score         DOUBLE PRECISION,
    start_time         TIMESTAMP,
    due_time           TIMESTAMP,
    attempt_limited    INTEGER,
    course_id          UUID         NOT NULL REFERENCES courses (id),
    student_id         UUID REFERENCES students (user_id),
    teacher_id         UUID REFERENCES teachers (user_id),
    display            BOOLEAN      NOT NULL DEFAULT true,
    lifecycle_status   VARCHAR(30)  NOT NULL DEFAULT 'PUBLISHED'
        CHECK (lifecycle_status IN ('DRAFT', 'REVIEW_PENDING', 'READY_TO_PUBLISH', 'PUBLISHED', 'ARCHIVED')),
    source_type        VARCHAR(30)  NOT NULL DEFAULT 'MANUAL'
        CHECK (source_type IN ('MANUAL', 'AI_GENERATED', 'CLONED')),
    original_quiz_id   UUID REFERENCES quizzes (id) ON DELETE SET NULL,
    version            BIGINT       NOT NULL DEFAULT 0 CHECK (version >= 0),
    published_by       UUID REFERENCES users (id) ON DELETE SET NULL,
    published_at       TIMESTAMP,
    created_at         TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_quizzes_course_lifecycle_updated
    ON quizzes (course_id, lifecycle_status, updated_at DESC, id);
CREATE INDEX IF NOT EXISTS idx_quizzes_original
    ON quizzes (original_quiz_id);

-- Durable business queue for asynchronous AI quiz generation. Quartz only
-- stores scheduler execution state; this table remains the source of truth.
CREATE TABLE IF NOT EXISTS quiz_generation_jobs
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id           UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    exam_matrix_id      UUID         NOT NULL REFERENCES exam_matrices (id) ON DELETE RESTRICT,
    quiz_id             UUID REFERENCES quizzes (id) ON DELETE SET NULL,
    requested_by        UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    requested_role      VARCHAR(50)  NOT NULL,
    quiz_title          VARCHAR(255),
    description         TEXT,
    prompt              TEXT,
    request_snapshot    JSONB        NOT NULL,
    payload_hash        VARCHAR(64)  NOT NULL,
    idempotency_key     VARCHAR(255),
    strict_mode         BOOLEAN      NOT NULL DEFAULT true,
    code_runner_policy  VARCHAR(50)  NOT NULL DEFAULT 'PRESERVE',
    unlimited           BOOLEAN,
    time_limit_minutes  INTEGER,
    pass_score          DOUBLE PRECISION,
    start_time          TIMESTAMP,
    due_time            TIMESTAMP,
    attempt_limited     INTEGER,
    display             BOOLEAN      NOT NULL DEFAULT false,
    phase               VARCHAR(30)  NOT NULL DEFAULT 'TEXT_GENERATION'
        CHECK (phase IN ('TEXT_GENERATION', 'VISUAL_GENERATION', 'FINALIZING', 'DONE')),
    status              VARCHAR(30)  NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'TIMED_OUT')),
    attempt_count       INTEGER      NOT NULL DEFAULT 0,
    generated_question_count INTEGER NOT NULL DEFAULT 0 CHECK (generated_question_count >= 0),
    total_question_count INTEGER NOT NULL DEFAULT 0 CHECK (total_question_count >= 0),
    next_attempt_at     TIMESTAMPTZ,
    lease_until         TIMESTAMPTZ,
    error_code          VARCHAR(100),
    error_message       TEXT,
    callback_event_id   UUID UNIQUE,
    processing_at       TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_quiz_generation_request UNIQUE (requested_by, idempotency_key)
);

ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE quiz_generation_jobs ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE quiz_generation_jobs ADD COLUMN IF NOT EXISTS generated_question_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE quiz_generation_jobs ADD COLUMN IF NOT EXISTS total_question_count INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_quiz_generation_queue
    ON quiz_generation_jobs (status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_quiz_generation_lease
    ON quiz_generation_jobs (status, lease_until);
CREATE INDEX IF NOT EXISTS idx_quiz_generation_requester
    ON quiz_generation_jobs (requested_by, created_at DESC);

-- Questions table
CREATE TABLE IF NOT EXISTS questions
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content             TEXT,
    content_format      VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT'
        CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    explanation        TEXT,
    explanation_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT'
        CHECK (explanation_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    type                VARCHAR(50),
    quiz_id             UUID REFERENCES quizzes (id) ON DELETE CASCADE,
    review_status       VARCHAR(30) NOT NULL DEFAULT 'APPROVED'
        CHECK (review_status IN ('PENDING_REVIEW', 'APPROVED', 'NEEDS_REVISION', 'REJECTED')),
    source_type         VARCHAR(30) NOT NULL DEFAULT 'MANUAL'
        CHECK (source_type IN ('MANUAL', 'BANK_COPY', 'AI_GENERATED', 'QUIZ_CLONE')),
    source_bank_question_id UUID REFERENCES question_bank_questions (id) ON DELETE SET NULL,
    source_question_id  UUID REFERENCES questions (id) ON DELETE SET NULL,
    version             BIGINT      NOT NULL DEFAULT 0 CHECK (version >= 0),
    reviewed_by         UUID REFERENCES users (id) ON DELETE SET NULL,
    reviewed_at         TIMESTAMP,
    created_at          TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_questions_quiz_review
    ON questions (quiz_id, review_status, created_at, id);
CREATE INDEX IF NOT EXISTS idx_questions_source_bank
    ON questions (source_bank_question_id);
CREATE INDEX IF NOT EXISTS idx_questions_source_question
    ON questions (source_question_id);

-- AI-only allocation history. Mutable planning entities use SET NULL while the
-- immutable allocation snapshot remains available for audit/review checks.
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

-- Quiz2 assessment workflow. Course CCCT profiles keep their existing
-- DRAFT/PUBLISHED/ARCHIVED lifecycle. Exam matrices gain an equivalent
-- immutable-published lifecycle so an assessment can retain a stable source.
ALTER TABLE exam_matrices
    ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (lifecycle_status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    ADD COLUMN IF NOT EXISTS published_by UUID REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS assessments
(
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id                       UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    title                           VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    description                     TEXT,
    unlimited                       BOOLEAN      NOT NULL DEFAULT false,
    time_limit_minutes              INTEGER CHECK (time_limit_minutes IS NULL OR time_limit_minutes > 0),
    pass_score                      DOUBLE PRECISION CHECK (pass_score IS NULL OR (pass_score >= 0 AND pass_score <= 100)),
    start_time                      TIMESTAMP,
    due_time                        TIMESTAMP,
    attempt_limited                 INTEGER CHECK (attempt_limited IS NULL OR attempt_limited > 0),
    variant_count                   INTEGER CHECK (variant_count BETWEEN 1 AND 20),
    option_count                    INTEGER CHECK (option_count IN (4, 5)),
    allow_recently_used_questions   BOOLEAN      NOT NULL DEFAULT false,
    lifecycle_status                VARCHAR(30)  NOT NULL DEFAULT 'DRAFT'
        CHECK (lifecycle_status IN ('DRAFT', 'GENERATING', 'REVIEW_PENDING', 'READY_TO_PUBLISH', 'PUBLISHED', 'ARCHIVED')),
    current_step                    INTEGER      NOT NULL DEFAULT 1 CHECK (current_step BETWEEN 1 AND 9),
    ccct_profile_id                 UUID REFERENCES ccct_profiles (id) ON DELETE RESTRICT,
    ccct_profile_version            BIGINT,
    exam_matrix_id                  UUID REFERENCES exam_matrices (id) ON DELETE RESTRICT,
    exam_matrix_version             BIGINT,
    previous_published_ccct_id      UUID REFERENCES ccct_profiles (id) ON DELETE SET NULL,
    ccct_published_by_assessment    BOOLEAN      NOT NULL DEFAULT false,
    matrix_published_by_assessment  BOOLEAN      NOT NULL DEFAULT false,
    ai_generation_started           BOOLEAN      NOT NULL DEFAULT false,
    base_generation_job_id          UUID REFERENCES quiz_generation_jobs (id) ON DELETE SET NULL,
    base_confirmed_by               UUID REFERENCES users (id) ON DELETE SET NULL,
    base_confirmed_at               TIMESTAMP,
    recent_use_override_by          UUID REFERENCES users (id) ON DELETE SET NULL,
    recent_use_override_at          TIMESTAMP,
    created_by                      UUID REFERENCES users (id) ON DELETE SET NULL,
    version                         BIGINT       NOT NULL DEFAULT 0 CHECK (version >= 0),
    published_by                    UUID REFERENCES users (id) ON DELETE SET NULL,
    published_at                    TIMESTAMP,
    last_unpublished_by             UUID REFERENCES users (id) ON DELETE SET NULL,
    last_unpublished_at             TIMESTAMP,
    created_at                      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (due_time IS NULL OR start_time IS NULL OR due_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_assessments_course_updated
    ON assessments (course_id, updated_at DESC, id);

CREATE TABLE IF NOT EXISTS assessment_generation_batches
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id      UUID         NOT NULL REFERENCES assessments (id) ON DELETE CASCADE,
    status             VARCHAR(20)  NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'PARTIAL', 'FAILED')),
    option_count       INTEGER      NOT NULL CHECK (option_count IN (4, 5)),
    total_cells        INTEGER      NOT NULL DEFAULT 0 CHECK (total_cells >= 0),
    completed_cells    INTEGER      NOT NULL DEFAULT 0 CHECK (completed_cells >= 0),
    generated_questions INTEGER     NOT NULL DEFAULT 0 CHECK (generated_questions >= 0),
    request_snapshot   JSONB        NOT NULL,
    idempotency_key    VARCHAR(255) NOT NULL UNIQUE,
    error_message      TEXT,
    created_by         UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_assessment_batches_parent
    ON assessment_generation_batches (assessment_id, created_at DESC);

CREATE TABLE IF NOT EXISTS assessment_webhook_events
(
    event_id    UUID PRIMARY KEY,
    batch_id    UUID NOT NULL REFERENCES assessment_generation_batches (id) ON DELETE CASCADE,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS assessment_generation_batch_items
(
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id                 UUID        NOT NULL REFERENCES assessment_generation_batches (id) ON DELETE CASCADE,
    matrix_row_id            UUID        NOT NULL REFERENCES exam_matrix_rows (id) ON DELETE RESTRICT,
    custom_learning_level_id UUID        NOT NULL REFERENCES custom_learning_levels (id) ON DELETE RESTRICT,
    question_type            VARCHAR(30) NOT NULL DEFAULT 'MULTIPLE_CHOICE'
        CHECK (question_type IN ('MULTIPLE_CHOICE', 'CODE_RUNNER')),
    required_count           INTEGER     NOT NULL CHECK (required_count > 0),
    generated_count          INTEGER     NOT NULL DEFAULT 0 CHECK (generated_count >= 0),
    status                   VARCHAR(20) NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    error_message            TEXT,
    retry_count              INTEGER     NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
    updated_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_assessment_batch_cell UNIQUE (batch_id, matrix_row_id, custom_learning_level_id)
);

ALTER TABLE question_drafts
    ADD COLUMN IF NOT EXISTS assessment_id UUID REFERENCES assessments (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS generation_batch_id UUID REFERENCES assessment_generation_batches (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS matrix_row_id UUID REFERENCES exam_matrix_rows (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS custom_learning_level_id UUID REFERENCES custom_learning_levels (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(20) NOT NULL DEFAULT 'NOT_VALIDATED'
        CHECK (review_status IN ('NOT_VALIDATED', 'REJECTED')),
    ADD COLUMN IF NOT EXISTS rejected_by UUID REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS supersedes_bank_question_id UUID REFERENCES question_bank_questions (id) ON DELETE SET NULL;

ALTER TABLE question_bank_questions
    ADD COLUMN IF NOT EXISTS assessment_id UUID REFERENCES assessments (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS generation_batch_id UUID REFERENCES assessment_generation_batches (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS matrix_row_id UUID REFERENCES exam_matrix_rows (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS custom_learning_level_id UUID REFERENCES custom_learning_levels (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS recently_used BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS supersedes_bank_question_id UUID REFERENCES question_bank_questions (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_question_drafts_assessment_review
    ON question_drafts (assessment_id, review_status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_question_bank_assessment
    ON question_bank_questions (assessment_id, validated_at DESC);
CREATE INDEX IF NOT EXISTS idx_question_bank_recently_used
    ON question_bank_questions (course_id, recently_used, validated_at DESC);

CREATE TABLE IF NOT EXISTS question_bank_allocations
(
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_bank_question_id UUID        NOT NULL REFERENCES question_bank_questions (id) ON DELETE RESTRICT,
    assessment_id             UUID        NOT NULL REFERENCES assessments (id) ON DELETE CASCADE,
    matrix_row_id             UUID REFERENCES exam_matrix_rows (id) ON DELETE SET NULL,
    custom_learning_level_id  UUID REFERENCES custom_learning_levels (id) ON DELETE SET NULL,
    status                    VARCHAR(20) NOT NULL
        CHECK (status IN ('RESERVED', 'USED', 'RELEASED')),
    reserved_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at                   TIMESTAMP,
    released_at               TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_question_bank_active_allocation
    ON question_bank_allocations (question_bank_question_id)
    WHERE status = 'RESERVED';
CREATE INDEX IF NOT EXISTS idx_question_bank_allocations_assessment
    ON question_bank_allocations (assessment_id, status);

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS chapter_id UUID REFERENCES chapters (chapter_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS cognitive_level_id UUID REFERENCES cognitive_levels (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS matrix_row_id UUID REFERENCES exam_matrix_rows (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS custom_learning_level_id UUID REFERENCES custom_learning_levels (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS order_index INTEGER NOT NULL DEFAULT 0 CHECK (order_index >= 0);

ALTER TABLE question_bank_allocations
    ADD COLUMN IF NOT EXISTS matrix_row_id UUID REFERENCES exam_matrix_rows (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS custom_learning_level_id UUID REFERENCES custom_learning_levels (id) ON DELETE SET NULL;

-- Backfill traces only when their Question Bank source already carries a recorded origin.
-- Any remaining NULL remains explicitly unrecorded rather than guessed from compatible Matrix cells.
UPDATE question_bank_allocations allocation
SET matrix_row_id = COALESCE(allocation.matrix_row_id, bank.matrix_row_id),
    custom_learning_level_id = COALESCE(allocation.custom_learning_level_id, bank.custom_learning_level_id)
FROM question_bank_questions bank
WHERE allocation.question_bank_question_id = bank.id
  AND (allocation.matrix_row_id IS NULL OR allocation.custom_learning_level_id IS NULL)
  AND (bank.matrix_row_id IS NOT NULL OR bank.custom_learning_level_id IS NOT NULL);

UPDATE questions snapshot
SET matrix_row_id = COALESCE(snapshot.matrix_row_id, bank.matrix_row_id),
    custom_learning_level_id = COALESCE(snapshot.custom_learning_level_id, bank.custom_learning_level_id)
FROM question_bank_questions bank
WHERE snapshot.source_bank_question_id = bank.id
  AND (snapshot.matrix_row_id IS NULL OR snapshot.custom_learning_level_id IS NULL)
  AND (bank.matrix_row_id IS NOT NULL OR bank.custom_learning_level_id IS NOT NULL);

CREATE TABLE IF NOT EXISTS assessment_variants
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id       UUID         NOT NULL REFERENCES assessments (id) ON DELETE CASCADE,
    quiz_id             UUID         NOT NULL UNIQUE REFERENCES quizzes (id) ON DELETE CASCADE,
    variant_code        VARCHAR(20)  NOT NULL,
    order_index         INTEGER      NOT NULL CHECK (order_index >= 0),
    shuffle_seed        BIGINT       NOT NULL,
    variant_kind        VARCHAR(20)  NOT NULL DEFAULT 'SHUFFLED'
        CHECK (variant_kind IN ('BASE', 'SHUFFLED')),
    coverage_status     VARCHAR(20)  NOT NULL DEFAULT 'COMPLIANT'
        CHECK (coverage_status IN ('COMPLIANT', 'DRIFTED')),
    coverage_snapshot   JSONB        NOT NULL DEFAULT '{}'::jsonb,
    override_reason     TEXT,
    override_by         UUID REFERENCES users (id) ON DELETE SET NULL,
    override_at         TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_assessment_variant_code UNIQUE (assessment_id, variant_code),
    CONSTRAINT uq_assessment_variant_order UNIQUE (assessment_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_assessment_variants_parent
    ON assessment_variants (assessment_id, order_index);
CREATE UNIQUE INDEX IF NOT EXISTS uq_assessment_base_variant
    ON assessment_variants (assessment_id) WHERE variant_kind = 'BASE';

CREATE TABLE IF NOT EXISTS question_images
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id      UUID         NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    source_type      VARCHAR(20)  NOT NULL DEFAULT 'UPLOAD'
        CHECK (source_type IN ('AUTO', 'LATEX', 'SVG', 'UPLOAD')),
    source_text      TEXT,
    generation_brief TEXT,
    alt_text         TEXT,
    status           VARCHAR(20)  NOT NULL DEFAULT 'READY'
        CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')),
    image_name       VARCHAR(255),
    storage_key      TEXT,
    url              TEXT,
    media_type       VARCHAR(255),
    content_hash     VARCHAR(64),
    byte_size        BIGINT,
    error_code       VARCHAR(100),
    error_message    TEXT,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK ((source_type = 'AUTO' AND status IN ('PENDING', 'PROCESSING', 'FAILED'))
        OR (source_type = 'LATEX' AND (status IN ('PENDING', 'PROCESSING', 'FAILED') OR source_text IS NOT NULL))
        OR (source_type = 'SVG' AND (status IN ('PENDING', 'PROCESSING', 'FAILED') OR source_text IS NOT NULL))
        OR (source_type = 'UPLOAD' AND (storage_key IS NOT NULL OR url IS NOT NULL)))
);

CREATE INDEX IF NOT EXISTS idx_question_images_question_id ON question_images (question_id);
UPDATE question_images SET source_type='UPLOAD' WHERE source_type IS NULL;

-- Embedded editor images are separate from legacy/AI visual metadata.
CREATE TABLE IF NOT EXISTS rich_content_assets
(
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_draft_id         UUID REFERENCES question_drafts (id) ON DELETE CASCADE,
    question_bank_question_id UUID REFERENCES question_bank_questions (id) ON DELETE CASCADE,
    question_id               UUID REFERENCES questions (id) ON DELETE CASCADE,
    storage_key               VARCHAR(1024) NOT NULL,
    media_type                VARCHAR(255)  NOT NULL,
    byte_size                 BIGINT        NOT NULL CHECK (byte_size >= 0 AND byte_size <= 10485760),
    sha256                    CHAR(64)      NOT NULL,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_rich_content_asset_exactly_one_owner CHECK (
        ((question_draft_id IS NOT NULL)::int + (question_bank_question_id IS NOT NULL)::int
            + (question_id IS NOT NULL)::int) = 1
    )
);
CREATE INDEX IF NOT EXISTS idx_rich_content_assets_draft
    ON rich_content_assets (question_draft_id) WHERE question_draft_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_rich_content_assets_bank
    ON rich_content_assets (question_bank_question_id) WHERE question_bank_question_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_rich_content_assets_question
    ON rich_content_assets (question_id) WHERE question_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS question_image_generation_jobs
(
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_generation_job_id UUID        NOT NULL REFERENCES quiz_generation_jobs (id) ON DELETE CASCADE,
    question_image_id      UUID        NOT NULL UNIQUE REFERENCES question_images (id) ON DELETE CASCADE,
    visual_type            VARCHAR(20) NOT NULL CHECK (visual_type IN ('AUTO', 'LATEX', 'SVG')),
    status                 VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    attempt_count          INTEGER     NOT NULL DEFAULT 0,
    next_attempt_at        TIMESTAMPTZ,
    lease_until            TIMESTAMPTZ,
    error_code             VARCHAR(100),
    error_message          TEXT,
    processing_at          TIMESTAMPTZ,
    completed_at           TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_question_image_generation_queue
    ON question_image_generation_jobs (status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_question_image_generation_parent
    ON question_image_generation_jobs (quiz_generation_job_id);

-- Upgrade visual constraints for databases created before authoritative image routing.
ALTER TABLE question_images DROP CONSTRAINT IF EXISTS question_images_source_type_check;
ALTER TABLE question_images ADD CONSTRAINT question_images_source_type_check
    CHECK (source_type IN ('AUTO', 'LATEX', 'SVG', 'UPLOAD'));
ALTER TABLE question_images DROP CONSTRAINT IF EXISTS question_images_check;
ALTER TABLE question_images ADD CONSTRAINT question_images_check
    CHECK ((source_type = 'AUTO' AND status IN ('PENDING', 'PROCESSING', 'FAILED'))
        OR (source_type = 'LATEX' AND (status IN ('PENDING', 'PROCESSING', 'FAILED') OR source_text IS NOT NULL))
        OR (source_type = 'SVG' AND (status IN ('PENDING', 'PROCESSING', 'FAILED') OR source_text IS NOT NULL))
        OR (source_type = 'UPLOAD' AND (storage_key IS NOT NULL OR url IS NOT NULL)));
ALTER TABLE question_image_generation_jobs
    DROP CONSTRAINT IF EXISTS question_image_generation_jobs_visual_type_check;
ALTER TABLE question_image_generation_jobs
    ADD CONSTRAINT question_image_generation_jobs_visual_type_check
    CHECK (visual_type IN ('AUTO', 'LATEX', 'SVG'));

-- Answers table (weak entity dependent on Question)
CREATE TABLE IF NOT EXISTS answers
(
    answer_number BIGINT GENERATED ALWAYS AS IDENTITY,
    content       TEXT,
    is_correct    BOOLEAN   DEFAULT false,
    explanation   TEXT,
    content_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT'
        CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    explanation_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT'
        CHECK (explanation_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    question_id   UUID NOT NULL REFERENCES questions (id),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (answer_number, question_id)
);

-- Immutable revisions of executable Code Runner contracts. Question review
-- remains the business approval source; contract_status records executable
-- verification of a revision. Private grading data never enters learner APIs.
CREATE TABLE IF NOT EXISTS code_runner_configs
(
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id                UUID         NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    schema_version             VARCHAR(20)  NOT NULL CHECK (schema_version = '2.0.0'),
    contract_id                VARCHAR(150) NOT NULL,
    contract_version           VARCHAR(20)  NOT NULL,
    contract_status            VARCHAR(50)  NOT NULL,
    title                      VARCHAR(500) NOT NULL CHECK (btrim(title) <> ''),
    statement                  TEXT         NOT NULL CHECK (btrim(statement) <> ''),
    execution_profile_id       VARCHAR(100) NOT NULL,
    execution_profile_version  VARCHAR(20)  NOT NULL,
    archetype_id               VARCHAR(100) NOT NULL,
    archetype_version          VARCHAR(20)  NOT NULL,
    comparator_id              VARCHAR(100) NOT NULL,
    comparator_version         VARCHAR(20)  NOT NULL,
    entry_point                VARCHAR(150) NOT NULL,
    signature_json             JSONB        NOT NULL,
    starter_code               TEXT         NOT NULL CHECK (btrim(starter_code) <> ''),
    banned_api_patterns_json   JSONB        NOT NULL DEFAULT '[]'::jsonb,
    reference_solution         TEXT         NOT NULL CHECK (btrim(reference_solution) <> ''),
    revision                   INTEGER      NOT NULL CHECK (revision > 0),
    created_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_code_runner_config_revision UNIQUE (question_id, revision)
);

CREATE INDEX IF NOT EXISTS idx_code_runner_configs_question_latest
    ON code_runner_configs (question_id, revision DESC);

CREATE TABLE IF NOT EXISTS code_runner_test_cases
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_runner_config_id UUID         NOT NULL REFERENCES code_runner_configs (id) ON DELETE CASCADE,
    external_test_id      VARCHAR(150) NOT NULL,
    category              VARCHAR(150) NOT NULL,
    visibility            VARCHAR(10)  NOT NULL CHECK (visibility IN ('PUBLIC', 'HIDDEN')),
    arguments_json        JSONB        NOT NULL,
    expected_output_json  JSONB        NOT NULL,
    points                DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (points >= 0),
    ordinal               INTEGER      NOT NULL CHECK (ordinal >= 0),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_code_runner_test_external UNIQUE (code_runner_config_id, external_test_id),
    CONSTRAINT uq_code_runner_test_ordinal UNIQUE (code_runner_config_id, ordinal),
    CONSTRAINT ck_code_runner_test_points CHECK (
        (visibility = 'PUBLIC' AND points = 0) OR
        (visibility = 'HIDDEN' AND points > 0)
    )
);

CREATE INDEX IF NOT EXISTS idx_code_runner_tests_config_visibility
    ON code_runner_test_cases (code_runner_config_id, visibility, ordinal);

-- A publish creates an immutable release. Attempts point at a release instead
-- of resolving whichever Code Runner revision happens to be newest later.
CREATE TABLE IF NOT EXISTS quiz_releases
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id        UUID      NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    release_number INTEGER   NOT NULL CHECK (release_number > 0),
    quiz_version   BIGINT    NOT NULL CHECK (quiz_version >= 0),
    active         BOOLEAN   NOT NULL DEFAULT true,
    published_by   UUID REFERENCES users (id) ON DELETE SET NULL,
    published_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_quiz_release_number UNIQUE (quiz_id, release_number),
    CONSTRAINT uq_quiz_release_identity UNIQUE (id, quiz_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_quiz_release_active
    ON quiz_releases (quiz_id) WHERE active = true;

CREATE TABLE IF NOT EXISTS quiz_release_code_runner_bindings
(
    quiz_release_id      UUID NOT NULL REFERENCES quiz_releases (id) ON DELETE CASCADE,
    question_id          UUID NOT NULL REFERENCES questions (id) ON DELETE RESTRICT,
    code_runner_config_id UUID NOT NULL REFERENCES code_runner_configs (id) ON DELETE RESTRICT,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (quiz_release_id, question_id),
    CONSTRAINT uq_quiz_release_config UNIQUE (quiz_release_id, code_runner_config_id)
);

CREATE INDEX IF NOT EXISTS idx_quiz_release_code_runner_question
    ON quiz_release_code_runner_bindings (question_id, quiz_release_id);

-- Attempts table
CREATE TABLE IF NOT EXISTS attempts
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id         UUID        NOT NULL REFERENCES students (user_id),
    quiz_id            UUID        NOT NULL REFERENCES quizzes (id),
    quiz_release_id    UUID        NOT NULL,
    score              DOUBLE PRECISION,
    start_time         TIMESTAMP,
    due_time           TIMESTAMP,
    submitted_time     TIMESTAMP,
    time_spent_seconds INTEGER,
    status             VARCHAR(50) NOT NULL CHECK (status IN ('STARTED', 'FINALIZING', 'FINISHED', 'ERROR')),
    error_reason       VARCHAR(255),
    created_at         TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attempt_quiz_release
        FOREIGN KEY (quiz_release_id, quiz_id)
        REFERENCES quiz_releases (id, quiz_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_attempts_student_quiz_status ON attempts (student_id, quiz_id, status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_attempts_student_quiz_active
    ON attempts (student_id, quiz_id)
    WHERE status IN ('STARTED', 'FINALIZING');
CREATE INDEX IF NOT EXISTS idx_attempts_release ON attempts (quiz_release_id);
CREATE INDEX IF NOT EXISTS idx_attempts_due_time ON attempts (due_time);

CREATE TABLE IF NOT EXISTS code_submissions
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id           UUID         NOT NULL REFERENCES questions (id) ON DELETE RESTRICT,
    code_runner_config_id UUID         NOT NULL REFERENCES code_runner_configs (id) ON DELETE RESTRICT,
    attempt_id            UUID         NOT NULL REFERENCES attempts (id) ON DELETE CASCADE,
    student_id            UUID         NOT NULL REFERENCES students (user_id) ON DELETE RESTRICT,
    source_code           TEXT         NOT NULL CHECK (btrim(source_code) <> ''),
    idempotency_key       VARCHAR(255) NOT NULL,
    submission_mode       VARCHAR(10)  NOT NULL CHECK (submission_mode IN ('RUN', 'SUBMIT')),
    status                VARCHAR(40)  NOT NULL CHECK (status IN ('PENDING', 'CLAIMED', 'PROCESSING', 'COMPLETED', 'FAILED', 'FAILED_SYSTEM')),
    verdict               VARCHAR(40),
    score                 DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (score >= 0),
    max_score             DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (max_score >= 0),
    worker_id             UUID,
    lease_until           TIMESTAMP WITH TIME ZONE,
    accepted_at           TIMESTAMP,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at          TIMESTAMP,
    attempt_count         INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at       TIMESTAMP,
    CONSTRAINT uq_code_submission_idempotency UNIQUE (student_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_code_submissions_attempt_question
    ON code_submissions (attempt_id, question_id, submission_mode, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_code_submissions_cooldown_lookup
    ON code_submissions (attempt_id, question_id, submission_mode, accepted_at DESC);
CREATE INDEX IF NOT EXISTS idx_code_submissions_processing
    ON code_submissions (status, created_at) WHERE status IN ('PENDING', 'PROCESSING');

-- The exact SUBMIT selected when a learner finishes an attempt. Later
-- submissions must never replace this grading snapshot.
CREATE TABLE IF NOT EXISTS attempt_code_submission_snapshots
(
    attempt_id     UUID NOT NULL REFERENCES attempts (id) ON DELETE CASCADE,
    question_id    UUID NOT NULL REFERENCES questions (id) ON DELETE RESTRICT,
    submission_id  UUID NOT NULL REFERENCES code_submissions (id) ON DELETE RESTRICT,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attempt_id, question_id),
    CONSTRAINT uq_attempt_code_submission_snapshot_submission UNIQUE (attempt_id, submission_id)
);

CREATE INDEX IF NOT EXISTS idx_attempt_code_submission_snapshots_submission
    ON attempt_code_submission_snapshots (submission_id);

CREATE TABLE IF NOT EXISTS code_submission_test_results
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id        UUID        NOT NULL REFERENCES code_submissions (id) ON DELETE CASCADE,
    test_case_id         UUID        NOT NULL REFERENCES code_runner_test_cases (id) ON DELETE RESTRICT,
    execution_status     VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (execution_status IN ('PENDING', 'PROCESSING', 'COMPLETED')),
    verdict              VARCHAR(40),
    points_earned        DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (points_earned >= 0),
    time_ms              DOUBLE PRECISION,
    memory_kb            BIGINT,
    diagnostic           TEXT,
    engine_submission_id VARCHAR(255),
    created_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_code_submission_test UNIQUE (submission_id, test_case_id)
);

-- Join tables for many-to-many relationships
CREATE TABLE IF NOT EXISTS quiz_chapters
(
    quiz_id    UUID REFERENCES quizzes (id) ON DELETE CASCADE,
    chapter_id UUID REFERENCES chapters (chapter_id) ON DELETE CASCADE,
    PRIMARY KEY (quiz_id, chapter_id)
);

CREATE TABLE IF NOT EXISTS quiz_learning_outcomes
(
    quiz_id             UUID REFERENCES quizzes (id) ON DELETE CASCADE,
    learning_outcome_id UUID REFERENCES learning_outcomes (id) ON DELETE CASCADE,
    PRIMARY KEY (quiz_id, learning_outcome_id)
);

CREATE TABLE IF NOT EXISTS question_learning_outcomes
(
    question_id         UUID REFERENCES questions (id) ON DELETE CASCADE,
    learning_outcome_id UUID REFERENCES learning_outcomes (id) ON DELETE CASCADE,
    PRIMARY KEY (question_id, learning_outcome_id)
);


-- Activity Logs table for History module
CREATE TABLE IF NOT EXISTS activity_logs
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id   UUID        NOT NULL,
    entity_name VARCHAR(255),
    description TEXT,
    created_at  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_activity_logs_user_time ON activity_logs (user_id, created_at DESC);

-- ==========================================
-- NOTIFICATION MODULE
-- ==========================================
CREATE TABLE IF NOT EXISTS notifications
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    message    TEXT         NOT NULL,
    type       VARCHAR(50), -- 'SYSTEM', 'COURSE_UPDATE', 'DEADLINE_REMINDER'
    link_url   VARCHAR(255),
    is_read    BOOLEAN          DEFAULT false,
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notif_user_read ON notifications (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notif_created_at ON notifications (created_at DESC);

-- ==========================================
-- CHAT MODULE
-- ==========================================
CREATE TABLE IF NOT EXISTS user_chat_messages
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipient_id   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content        TEXT NOT NULL,
    status         VARCHAR(50)      DEFAULT 'SENT' CHECK (status IN ('SENT', 'DELIVERED', 'READ')),
    client_temp_id VARCHAR(255),
    created_at     TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_chat_messages_participants ON user_chat_messages (sender_id, recipient_id, created_at DESC);

-- ==========================================
-- COURSE ANNOUNCEMENT MODULE
-- ==========================================
CREATE TABLE IF NOT EXISTS course_announcements
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id  UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    teacher_id UUID         NOT NULL REFERENCES users (id),
    type       VARCHAR(50)      DEFAULT 'INFO',
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_course_announcements_course_id ON course_announcements (course_id);

-- ==========================================
-- SPRING MODULITH EVENT PUBLICATION
-- ==========================================
CREATE TABLE IF NOT EXISTS event_publication
(
    id               UUID                     NOT NULL,
    completion_date  TIMESTAMP WITH TIME ZONE,
    event_type       VARCHAR(512)             NOT NULL,
    listener_id      VARCHAR(512)             NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    serialized_event VARCHAR(4000)            NOT NULL,
    PRIMARY KEY (id)
);

-- ==========================================
-- AI CHAT SESSIONS (Phiên chat / Threads)
-- ==========================================
CREATE TABLE IF NOT EXISTS ai_chat_sessions
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id  UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    title      VARCHAR(255)     DEFAULT 'Đoạn chat mới',
    is_pinned  BOOLEAN          DEFAULT FALSE,
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_chat_sessions_user_course ON ai_chat_sessions (user_id, course_id, updated_at DESC);

-- ==========================================
-- AI COURSE ASSISTANT CHAT MESSAGES
-- ==========================================
CREATE TABLE IF NOT EXISTS ai_course_chat_messages
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID        NOT NULL REFERENCES ai_chat_sessions (id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id  UUID        NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL, -- 'user' or 'assistant'
    content    TEXT        NOT NULL,
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_session ON ai_course_chat_messages (session_id, created_at ASC);

-- Fresh-database companion for the additive V20260813_02 migration. Existing
-- databases must use the migration and must not rerun this destructive script.
CREATE TABLE IF NOT EXISTS question_ai_working_drafts (
    question_id UUID PRIMARY KEY REFERENCES questions(id) ON DELETE CASCADE,
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES assessment_variants(id) ON DELETE CASCADE,
    base_question_version BIGINT NOT NULL CHECK (base_question_version >= 0),
    content TEXT NOT NULL,
    content_format VARCHAR(30) NOT NULL CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    applied_change_index INTEGER NOT NULL DEFAULT 0 CHECK (applied_change_index >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question_ai_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES assessment_variants(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    requested_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    mode VARCHAR(20) NOT NULL CHECK (mode IN ('ORCHESTRATE', 'TEXT', 'LATEX', 'SVG')),
    status VARCHAR(40) NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'NEEDS_CLARIFICATION',
        'APPLIED_TO_WORKING_DRAFT', 'FAILED', 'CANCELLED', 'TIMED_OUT')),
    phase VARCHAR(30) NOT NULL DEFAULT 'QUEUED' CHECK (phase IN ('QUEUED', 'ORCHESTRATOR', 'TEXT',
        'LATEX', 'SVG', 'VALIDATOR', 'CRITIC', 'APPLYING', 'DONE')),
    prompt TEXT NOT NULL DEFAULT '',
    request_payload JSONB NOT NULL,
    action_plan JSONB,
    base_question_version BIGINT NOT NULL CHECK (base_question_version >= 0),
    clarification_question TEXT,
    clarification_answer TEXT,
    error_code VARCHAR(80),
    error_message TEXT,
    warning_message TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count BETWEEN 0 AND 2),
    action_count INTEGER NOT NULL DEFAULT 0 CHECK (action_count BETWEEN 0 AND 12),
    lease_until TIMESTAMP,
    clarification_expires_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_question_ai_active_job ON question_ai_jobs(question_id)
    WHERE status IN ('QUEUED', 'RUNNING', 'NEEDS_CLARIFICATION');
CREATE INDEX IF NOT EXISTS idx_question_ai_job_queue ON question_ai_jobs(status, created_at);
CREATE INDEX IF NOT EXISTS idx_question_ai_job_timeline ON question_ai_jobs(question_id, created_at DESC);

CREATE TABLE IF NOT EXISTS question_ai_job_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES question_ai_jobs(id) ON DELETE CASCADE,
    action_id VARCHAR(64),
    step_order INTEGER NOT NULL CHECK (step_order BETWEEN 0 AND 12),
    specialist VARCHAR(30) NOT NULL CHECK (specialist IN ('ORCHESTRATOR', 'TEXT', 'LATEX', 'SVG', 'VALIDATOR', 'CRITIC')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED', 'WARNING')),
    attempt INTEGER NOT NULL DEFAULT 1 CHECK (attempt BETWEEN 1 AND 2),
    input_payload JSONB,
    output_payload JSONB,
    error_message TEXT,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_question_ai_steps_job ON question_ai_job_steps(job_id, step_order, started_at);

CREATE TABLE IF NOT EXISTS question_ai_artifacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    artifact_type VARCHAR(10) NOT NULL CHECK (artifact_type IN ('SVG', 'LATEX')),
    current_revision INTEGER NOT NULL DEFAULT 0 CHECK (current_revision >= 0),
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_question_ai_artifact_owner ON question_ai_artifacts(question_id, created_at);

CREATE TABLE IF NOT EXISTS question_ai_artifact_revisions (
    artifact_id UUID NOT NULL REFERENCES question_ai_artifacts(id) ON DELETE CASCADE,
    revision INTEGER NOT NULL CHECK (revision > 0),
    job_id UUID REFERENCES question_ai_jobs(id) ON DELETE SET NULL,
    source_code TEXT NOT NULL,
    source_hash VARCHAR(64) NOT NULL,
    latex_mode VARCHAR(10) CHECK (latex_mode IN ('INLINE', 'BLOCK')),
    visual_kind VARCHAR(30),
    generator VARCHAR(20) CHECK (generator IN ('SPECIALIZED', 'GENERIC')),
    structured_spec JSONB,
    layout_override JSONB,
    prompt TEXT,
    critic_result JSONB,
    fallback_metadata JSONB,
    storage_key VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (artifact_id, revision)
);

CREATE TABLE IF NOT EXISTS question_ai_change_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    job_id UUID REFERENCES question_ai_jobs(id) ON DELETE SET NULL,
    change_index INTEGER NOT NULL CHECK (change_index > 0),
    before_content TEXT NOT NULL,
    before_content_format VARCHAR(30) NOT NULL,
    after_content TEXT NOT NULL,
    after_content_format VARCHAR(30) NOT NULL,
    artifact_revisions JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(question_id, change_index)
);

ALTER TABLE rich_content_assets ADD COLUMN IF NOT EXISTS ai_artifact_id UUID
    REFERENCES question_ai_artifacts(id) ON DELETE SET NULL;


-- Quiz3 is an additive Matrix2 workflow. It deliberately does not reference
-- Quiz2 assessments, generation batches, drafts, or Question Bank allocation.
CREATE TABLE IF NOT EXISTS quiz3_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL CHECK (btrim(title)<>''), description TEXT, unlimited BOOLEAN NOT NULL DEFAULT false,
    time_limit_minutes INTEGER CHECK(time_limit_minutes IS NULL OR time_limit_minutes>0),
    pass_score DOUBLE PRECISION CHECK(pass_score IS NULL OR(pass_score>=0 AND pass_score<=100)),
    start_time TIMESTAMP, due_time TIMESTAMP, attempt_limited INTEGER CHECK(attempt_limited IS NULL OR attempt_limited>0),
    variant_count INTEGER NOT NULL DEFAULT 1 CHECK(variant_count BETWEEN 1 AND 20),
    lifecycle_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK(lifecycle_status IN('DRAFT','GENERATING','REVIEW_PENDING','READY_TO_PUBLISH','PUBLISHED','ARCHIVED')),
    current_step INTEGER NOT NULL DEFAULT 1 CHECK(current_step BETWEEN 1 AND 9),
    ccct_profile_id UUID REFERENCES ccct_profiles(id) ON DELETE RESTRICT, ccct_profile_version BIGINT,
    matrix2_id UUID REFERENCES exam_matrices_2(id) ON DELETE RESTRICT, matrix2_version BIGINT, base_generation_job_id UUID,
    base_confirmed_by UUID REFERENCES users(id) ON DELETE SET NULL, base_confirmed_at TIMESTAMP,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL, version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0),
    published_by UUID REFERENCES users(id) ON DELETE SET NULL, published_at TIMESTAMP,
    last_unpublished_by UUID REFERENCES users(id) ON DELETE SET NULL, last_unpublished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK(due_time IS NULL OR start_time IS NULL OR due_time>start_time)
);
CREATE INDEX IF NOT EXISTS idx_quiz3_assessments_course_updated ON quiz3_assessments(course_id,updated_at DESC,id);

CREATE TABLE IF NOT EXISTS quiz3_generation_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), assessment_id UUID NOT NULL REFERENCES quiz3_assessments(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE, matrix2_id UUID NOT NULL REFERENCES exam_matrices_2(id) ON DELETE RESTRICT,
    matrix2_version BIGINT NOT NULL, quiz_id UUID REFERENCES quizzes(id) ON DELETE SET NULL,
    requested_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT, request_snapshot JSONB NOT NULL,
    payload_hash VARCHAR(64) NOT NULL, idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK(status IN('PENDING','PROCESSING','COMPLETED','FAILED','TIMED_OUT')),
    phase VARCHAR(30) NOT NULL DEFAULT 'TEXT_GENERATION' CHECK(phase IN('TEXT_GENERATION','FINALIZING','DONE')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK(attempt_count>=0), generated_question_count INTEGER NOT NULL DEFAULT 0 CHECK(generated_question_count>=0),
    total_question_count INTEGER NOT NULL CHECK(total_question_count>0), next_attempt_at TIMESTAMPTZ, lease_until TIMESTAMPTZ,
    error_code VARCHAR(100), error_message TEXT, callback_event_id UUID UNIQUE, processing_at TIMESTAMPTZ, completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE quiz3_assessments DROP CONSTRAINT IF EXISTS quiz3_assessments_base_generation_job_id_fkey;
ALTER TABLE quiz3_assessments ADD CONSTRAINT quiz3_assessments_base_generation_job_id_fkey
    FOREIGN KEY(base_generation_job_id) REFERENCES quiz3_generation_jobs(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_quiz3_generation_queue ON quiz3_generation_jobs(status,next_attempt_at);

CREATE TABLE IF NOT EXISTS quiz3_assessment_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), assessment_id UUID NOT NULL REFERENCES quiz3_assessments(id) ON DELETE CASCADE,
    quiz_id UUID NOT NULL UNIQUE REFERENCES quizzes(id) ON DELETE CASCADE, variant_code VARCHAR(20) NOT NULL,
    order_index INTEGER NOT NULL CHECK(order_index>=0), shuffle_seed BIGINT NOT NULL,
    variant_kind VARCHAR(20) NOT NULL CHECK(variant_kind IN('BASE','SHUFFLED')),
    coverage_status VARCHAR(20) NOT NULL DEFAULT 'COMPLIANT' CHECK(coverage_status IN('COMPLIANT','DRIFTED')),
    coverage_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(assessment_id,variant_code),UNIQUE(assessment_id,order_index)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_quiz3_base_variant ON quiz3_assessment_variants(assessment_id) WHERE variant_kind='BASE';

CREATE TABLE IF NOT EXISTS quiz3_question_provenance (
    question_id UUID PRIMARY KEY REFERENCES questions(id) ON DELETE CASCADE,
    generation_job_id UUID NOT NULL REFERENCES quiz3_generation_jobs(id) ON DELETE RESTRICT,
    matrix2_row_id UUID REFERENCES exam_matrix_2_rows(id) ON DELETE SET NULL,
    matrix2_custom_learning_level_id UUID REFERENCES exam_matrix_2_custom_learning_levels(id) ON DELETE SET NULL,
    learning_outcome_id UUID REFERENCES learning_outcomes(id) ON DELETE SET NULL,
    source_question_id UUID REFERENCES questions(id) ON DELETE SET NULL, allocation_snapshot JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_quiz3_provenance_allocation ON quiz3_question_provenance(matrix2_row_id,matrix2_custom_learning_level_id,learning_outcome_id);

CREATE TABLE IF NOT EXISTS quiz3_question_ai_working_drafts (
    question_id UUID PRIMARY KEY REFERENCES questions(id) ON DELETE CASCADE,
    assessment_id UUID NOT NULL REFERENCES quiz3_assessments(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES quiz3_assessment_variants(id) ON DELETE CASCADE,
    base_question_version BIGINT NOT NULL CHECK(base_question_version>=0), content TEXT NOT NULL, content_format VARCHAR(30) NOT NULL,
    applied_change_index INTEGER NOT NULL DEFAULT 0 CHECK(applied_change_index>=0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS quiz3_question_ai_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),assessment_id UUID NOT NULL REFERENCES quiz3_assessments(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES quiz3_assessment_variants(id) ON DELETE CASCADE,question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    requested_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,mode VARCHAR(20) NOT NULL CHECK(mode IN('ORCHESTRATE','TEXT','LATEX','SVG')),
    status VARCHAR(40) NOT NULL CHECK(status IN('QUEUED','RUNNING','NEEDS_CLARIFICATION','APPLIED_TO_WORKING_DRAFT','FAILED','CANCELLED','TIMED_OUT')),
    phase VARCHAR(30) NOT NULL DEFAULT 'QUEUED',prompt TEXT NOT NULL DEFAULT '',request_payload JSONB NOT NULL,action_plan JSONB,
    base_question_version BIGINT NOT NULL,clarification_question TEXT,clarification_answer TEXT,error_code VARCHAR(80),error_message TEXT,
    warning_message TEXT,attempt_count INTEGER NOT NULL DEFAULT 0,action_count INTEGER NOT NULL DEFAULT 0,lease_until TIMESTAMP,
    clarification_expires_at TIMESTAMP,started_at TIMESTAMP,completed_at TIMESTAMP,created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_quiz3_question_ai_active_job ON quiz3_question_ai_jobs(question_id) WHERE status IN('QUEUED','RUNNING','NEEDS_CLARIFICATION');
CREATE TABLE IF NOT EXISTS quiz3_question_ai_job_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),job_id UUID NOT NULL REFERENCES quiz3_question_ai_jobs(id) ON DELETE CASCADE,
    action_id VARCHAR(64),step_order INTEGER NOT NULL,specialist VARCHAR(30) NOT NULL,status VARCHAR(20) NOT NULL,attempt INTEGER NOT NULL DEFAULT 1,
    input_payload JSONB,output_payload JSONB,error_message TEXT,started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,completed_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS quiz3_question_ai_artifacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),assessment_id UUID NOT NULL REFERENCES quiz3_assessments(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,artifact_type VARCHAR(10) NOT NULL,current_revision INTEGER NOT NULL DEFAULT 0,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS quiz3_question_ai_artifact_revisions (
    artifact_id UUID NOT NULL REFERENCES quiz3_question_ai_artifacts(id) ON DELETE CASCADE,revision INTEGER NOT NULL,
    job_id UUID REFERENCES quiz3_question_ai_jobs(id) ON DELETE SET NULL,source_code TEXT NOT NULL,source_hash VARCHAR(64) NOT NULL,
    latex_mode VARCHAR(10),visual_kind VARCHAR(30),generator VARCHAR(20),structured_spec JSONB,layout_override JSONB,prompt TEXT,
    critic_result JSONB,fallback_metadata JSONB,storage_key VARCHAR(1024),created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(artifact_id,revision)
);
CREATE TABLE IF NOT EXISTS quiz3_question_ai_change_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    job_id UUID REFERENCES quiz3_question_ai_jobs(id) ON DELETE SET NULL,change_index INTEGER NOT NULL,
    before_content TEXT NOT NULL,before_content_format VARCHAR(30) NOT NULL,after_content TEXT NOT NULL,after_content_format VARCHAR(30) NOT NULL,
    artifact_revisions JSONB NOT NULL DEFAULT '[]'::jsonb,created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE(question_id,change_index)
);

CREATE TABLE IF NOT EXISTS code_runner_verifications
(
    id                    UUID PRIMARY KEY,
    revision_id           UUID        NOT NULL,
    verified_by           UUID        NOT NULL,
    verified_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    execution_profile     JSONB       NOT NULL,
    judge0_submission_id  VARCHAR(255),
    verdict               VARCHAR(50) NOT NULL,
    stdout                TEXT,
    stderr                TEXT,
    time_ms               DOUBLE PRECISION,
    memory_kb             BIGINT,
    wall_time_limit_ms    INT,
    memory_limit_kb       INT
);

CREATE INDEX IF NOT EXISTS idx_code_runner_verifications_revision
    ON code_runner_verifications (revision_id);

CREATE TABLE IF NOT EXISTS code_runner_verification_cases
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    verification_id  UUID        NOT NULL REFERENCES code_runner_verifications (id) ON DELETE CASCADE,
    external_test_id VARCHAR(255) NOT NULL,
    visibility       VARCHAR(10) NOT NULL,
    verdict          VARCHAR(40) NOT NULL,
    time_ms          DOUBLE PRECISION,
    memory_kb        BIGINT,
    diagnostic       TEXT
);

CREATE INDEX IF NOT EXISTS idx_code_runner_verification_cases_verification
    ON code_runner_verification_cases (verification_id);
