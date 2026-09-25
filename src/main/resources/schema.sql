-- Local development uses spring.sql.init.mode=always. Rebuild only the LMS
-- schema so every backend restart starts from the schema and fixtures in this
-- repository. ai_index deliberately lives outside edu and survives.
DROP SCHEMA IF EXISTS edu CASCADE;
CREATE SCHEMA edu;

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
    original_file_name VARCHAR(255) NOT NULL,
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

-- Reusable course assets. Child tables carry type-specific payloads so CODE and
-- FORMULA can be added without changing the common lifecycle contract.
CREATE TABLE IF NOT EXISTS assets
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('IMAGE', 'CODE', 'FORMULA')),
    name VARCHAR(255) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    prompt TEXT,
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    publication_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (publication_status IN ('DRAFT', 'PUBLISHED')),
    upload_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (upload_status IN ('PENDING', 'UPLOADED', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_assets_course_type_status ON assets(course_id, type, publication_status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_assets_tags ON assets USING GIN(tags);

CREATE TABLE IF NOT EXISTS image_assets
(
    asset_id UUID PRIMARY KEY REFERENCES assets (id) ON DELETE CASCADE,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL CHECK (content_type IN ('image/png', 'image/jpeg', 'image/gif', 'image/svg+xml', 'image/webp')),
    byte_size BIGINT NOT NULL DEFAULT 0,
    storage_key VARCHAR(1024) NOT NULL UNIQUE,
    pending_original_file_name VARCHAR(255),
    pending_content_type VARCHAR(100) CHECK (pending_content_type IS NULL OR pending_content_type IN ('image/png', 'image/jpeg', 'image/gif', 'image/svg+xml', 'image/webp')),
    pending_storage_key VARCHAR(1024) UNIQUE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_material_uploaded_original_file_name_per_course
    ON materials (course_id, LOWER(BTRIM(original_file_name)))
    WHERE status = 'UPLOADED';

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
    kt_learning_outcome_id    BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE NOT NULL,
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
        CHECK (last_mode IN ('STRUCTURED', 'MARKDOWN')),
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
    order_index       INTEGER      NOT NULL CHECK (order_index >= 0),
    CONSTRAINT uq_ccct_chapter_order UNIQUE (profile_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_ccct_chapters_profile
    ON ccct_chapters (profile_id);
CREATE INDEX IF NOT EXISTS idx_ccct_chapters_course_chapter
    ON ccct_chapters (course_chapter_id);

CREATE TABLE IF NOT EXISTS ccct_topics
(
    id          UUID PRIMARY KEY,
    profile_id  UUID         NOT NULL REFERENCES ccct_profiles (id) ON DELETE CASCADE,
    chapter_id  UUID         NOT NULL REFERENCES ccct_chapters (id) ON DELETE CASCADE,
    group_code  VARCHAR(64)  NOT NULL CHECK (group_code ~ '^[a-z0-9]+([-_][a-z0-9]+)*$'),
    title       VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    order_index INTEGER      NOT NULL CHECK (order_index >= 0),
    CONSTRAINT uq_ccct_topic_order UNIQUE (chapter_id, order_index),
    CONSTRAINT uq_ccct_topic_group_code UNIQUE (profile_id, group_code)
);

CREATE INDEX IF NOT EXISTS idx_ccct_topics_chapter
    ON ccct_topics (chapter_id);

CREATE TABLE IF NOT EXISTS ccct_items
(
    id              UUID PRIMARY KEY,
    topic_id        UUID        NOT NULL REFERENCES ccct_topics (id) ON DELETE CASCADE,
    content_code    VARCHAR(128) NOT NULL,
    content         TEXT        NOT NULL CHECK (btrim(content) <> ''),
    order_index     INTEGER     NOT NULL CHECK (order_index >= 0),
    CONSTRAINT uq_ccct_item_order UNIQUE (topic_id, order_index),
    CONSTRAINT ck_ccct_item_content_code CHECK (content_code ~ '^[a-z0-9]+([._-][a-z0-9]+)*$'),
    CONSTRAINT uq_ccct_item_code UNIQUE (topic_id, content_code)
);

CREATE INDEX IF NOT EXISTS idx_ccct_items_topic
    ON ccct_items (topic_id);

-- Unified course exam matrices. MANUAL stores positive quotas; AUTOMATIC stores
-- selected cells and allocates the matrix-owned total deterministically.
CREATE TABLE IF NOT EXISTS exam_matrices
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id   UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    ccct_profile_id UUID     REFERENCES ccct_profiles (id) ON DELETE RESTRICT,
    ccct_profile_version BIGINT CHECK (ccct_profile_version >= 0),
    title       VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    description TEXT, mode VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK(mode IN ('MANUAL','AUTOMATIC')),
    total_question_count INTEGER CHECK(total_question_count IS NULL OR total_question_count > 0),
    lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (lifecycle_status IN ('DRAFT', 'PUBLISHED')),
    version     BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    published_by UUID REFERENCES users (id) ON DELETE SET NULL,
    published_at TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
    CHECK ((mode='MANUAL' AND total_question_count IS NULL) OR (mode='AUTOMATIC' AND total_question_count IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS exam_matrix_custom_learning_levels
(
    id          UUID PRIMARY KEY,
    matrix_id   UUID         NOT NULL REFERENCES exam_matrices (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL CHECK (btrim(name) <> ''),
    sort_order  INTEGER      NOT NULL CHECK (sort_order >= 0)
);

CREATE TABLE IF NOT EXISTS exam_matrix_outcome_cognitive_levels
(
    matrix_id          UUID        NOT NULL REFERENCES exam_matrices (id) ON DELETE CASCADE,
    learning_outcome_id UUID       NOT NULL REFERENCES learning_outcomes (id) ON DELETE CASCADE,
    cognitive_level_code VARCHAR(30) NOT NULL CHECK
        (cognitive_level_code IN ('REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE', 'EVALUATE', 'CREATE')),
    PRIMARY KEY (matrix_id, learning_outcome_id)
);

-- LMS-only presentation/allocation metadata for a Matrix-owned CLL. Quiz3's
-- generation snapshot deliberately does not read this table yet.
CREATE TABLE IF NOT EXISTS exam_matrix_custom_learning_level_asset_coverages
(
    custom_learning_level_id UUID        NOT NULL REFERENCES exam_matrix_custom_learning_levels (id) ON DELETE CASCADE,
    asset_type               VARCHAR(30) NOT NULL CHECK (asset_type IN ('TEXT', 'IMAGE', 'CODE_MCQ', 'CODE_RUNNER')),
    percentage               INTEGER     NOT NULL CHECK (percentage BETWEEN 0 AND 100),
    PRIMARY KEY (custom_learning_level_id, asset_type)
);

CREATE TABLE IF NOT EXISTS exam_matrix_rows
(
    id                         UUID PRIMARY KEY,
    matrix_id                  UUID NOT NULL REFERENCES exam_matrices (id) ON DELETE CASCADE,
    chapter_id                 UUID REFERENCES chapters (chapter_id) ON DELETE SET NULL,
    ccct_item_id               UUID REFERENCES ccct_items (id) ON DELETE RESTRICT,
    mapped_learning_outcome_id UUID REFERENCES learning_outcomes (id) ON DELETE SET NULL,
    sort_order                 INTEGER NOT NULL CHECK (sort_order >= 0),
    CONSTRAINT uq_exam_matrix_row_content UNIQUE (matrix_id, ccct_item_id)
);

CREATE TABLE IF NOT EXISTS exam_matrix_cells
(
    row_id                     UUID NOT NULL REFERENCES exam_matrix_rows (id) ON DELETE CASCADE,
    custom_learning_level_id   UUID NOT NULL REFERENCES exam_matrix_custom_learning_levels (id) ON DELETE CASCADE,
    question_count             INTEGER CHECK (question_count >= 0),
    PRIMARY KEY (row_id, custom_learning_level_id),
    CONSTRAINT uq_exam_matrix_cell_per_row UNIQUE (row_id) DEFERRABLE INITIALLY DEFERRED
);
CREATE INDEX IF NOT EXISTS exam_matrices_course_idx ON exam_matrices (course_id);
CREATE INDEX IF NOT EXISTS exam_matrix_rows_matrix_idx ON exam_matrix_rows (matrix_id, sort_order);

-- QBank2 is an independent, course-owned collection of plain-text content groups and examples.
-- This development/reset schema is executed with sql.init.mode=always.  Reset
-- QBank2 explicitly so the fixed fixtures in data.sql remain repeatable.
DROP TABLE IF EXISTS question_bank_2_examples;
DROP TABLE IF EXISTS question_bank_2_groups;
DROP TABLE IF EXISTS question_bank_2_code_counters;
CREATE TABLE IF NOT EXISTS question_bank_2_code_counters (
    course_id UUID PRIMARY KEY REFERENCES courses(id) ON DELETE CASCADE,
    next_value INTEGER NOT NULL CHECK(next_value > 0)
);
CREATE TABLE IF NOT EXISTS question_bank_2_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    code VARCHAR(64) NOT NULL, code_sequence INTEGER NOT NULL CHECK(code_sequence > 0),
    name VARCHAR(255) NOT NULL CHECK(btrim(name)<>''), description TEXT NOT NULL CHECK(btrim(description)<>''),
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(course_id,code), UNIQUE(course_id,code_sequence)
);
CREATE TABLE IF NOT EXISTS question_bank_2_examples (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), group_id UUID NOT NULL REFERENCES question_bank_2_groups(id) ON DELETE CASCADE,
    content TEXT NOT NULL CHECK(btrim(content)<>''), sequence INTEGER NOT NULL CHECK(sequence > 0),
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id,sequence)
);
CREATE INDEX IF NOT EXISTS question_bank_2_groups_course_idx ON question_bank_2_groups(course_id,code_sequence);
CREATE INDEX IF NOT EXISTS question_bank_2_examples_group_idx ON question_bank_2_examples(group_id,sequence);

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

-- Shared manual and Quiz3 quiz runtime.
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

-- Published and draft quiz questions.
CREATE TABLE IF NOT EXISTS questions
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kt_question_id      BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE NOT NULL,
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
    source_question_id  UUID REFERENCES questions (id) ON DELETE SET NULL,
    chapter_id          UUID REFERENCES chapters (chapter_id) ON DELETE SET NULL,
    cognitive_level_id  UUID REFERENCES cognitive_levels (id) ON DELETE SET NULL,
    order_index         INTEGER NOT NULL DEFAULT 0 CHECK (order_index >= 0),
    version             BIGINT      NOT NULL DEFAULT 0 CHECK (version >= 0),
    reviewed_by         UUID REFERENCES users (id) ON DELETE SET NULL,
    reviewed_at         TIMESTAMP,
    created_at          TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_questions_quiz_review
    ON questions (quiz_id, review_status, created_at, id);
CREATE INDEX IF NOT EXISTS idx_questions_source_question
    ON questions (source_question_id);

-- Question media is retained for manual quiz editing and existing quiz snapshots.
CREATE TABLE IF NOT EXISTS quiz_question_media
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id      UUID NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    source_type      VARCHAR(20) NOT NULL DEFAULT 'UPLOAD'
        CHECK (source_type IN ('AUTO', 'LATEX', 'SVG', 'UPLOAD')),
    source_text      TEXT,
    generation_brief TEXT,
    alt_text         TEXT,
    status           VARCHAR(20) NOT NULL DEFAULT 'READY'
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
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_quiz_question_media_question_id
    ON quiz_question_media (question_id);

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
    wall_time_limit_ms         INTEGER      NOT NULL DEFAULT 5000 CHECK (wall_time_limit_ms BETWEEN 100 AND 30000),
    memory_limit_kb            INTEGER      NOT NULL DEFAULT 131072 CHECK (memory_limit_kb BETWEEN 16384 AND 1048576),
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

-- Question-level grading snapshots used to construct learner-model histories.
-- UUID and numeric identifiers are deliberately snapshotted so later edits or
-- deletes do not rewrite the sequence observed when the attempt was submitted.
CREATE TABLE IF NOT EXISTS attempt_records
(
    attempt_id                    UUID    NOT NULL REFERENCES attempts (id) ON DELETE CASCADE,
    question_id                   UUID    NOT NULL,
    kt_question_id                BIGINT  NOT NULL,
    interaction_order             INTEGER NOT NULL CHECK (interaction_order >= 0),
    question_type                VARCHAR(40),
    question_content             TEXT,
    question_content_format      VARCHAR(40),
    student_answer_number        BIGINT,
    student_answer_position      INTEGER,
    student_answer_content       TEXT,
    student_answer_content_format VARCHAR(40),
    correct_answer_number        BIGINT,
    correct_answer_position      INTEGER,
    correct_answer_content       TEXT,
    correct_answer_content_format VARCHAR(40),
    is_correct                    BOOLEAN NOT NULL,
    learning_outcome_id           UUID,
    kt_learning_outcome_id        BIGINT,
    learning_outcome_mapping_count INTEGER NOT NULL DEFAULT 0
        CHECK (learning_outcome_mapping_count >= 0),
    snapshot_complete             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attempt_id, question_id),
    CONSTRAINT uq_attempt_record_order UNIQUE (attempt_id, interaction_order),
    CONSTRAINT ck_attempt_record_mapping CHECK (
        (learning_outcome_mapping_count = 1
            AND learning_outcome_id IS NOT NULL
            AND kt_learning_outcome_id IS NOT NULL)
        OR
        (learning_outcome_mapping_count <> 1
            AND learning_outcome_id IS NULL
            AND kt_learning_outcome_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_attempt_records_attempt_order
    ON attempt_records (attempt_id, interaction_order);

-- Immutable aggregate learner-model state after each finished attempt.
CREATE TABLE IF NOT EXISTS learner_states
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id    UUID      NOT NULL,
    course_id     UUID      NOT NULL,
    attempt_id    UUID      NOT NULL REFERENCES attempts (id) ON DELETE CASCADE,
    calculated_at TIMESTAMP NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_learner_states_attempt UNIQUE (attempt_id)
);

CREATE INDEX IF NOT EXISTS idx_learner_states_student_course_calculated
    ON learner_states (student_id, course_id, calculated_at, id);

CREATE TABLE IF NOT EXISTS learner_state_outcomes
(
    learner_state_id    UUID        NOT NULL REFERENCES learner_states (id) ON DELETE CASCADE,
    learning_outcome_id UUID        NOT NULL,
    probability         DOUBLE PRECISION,
    prediction_status   VARCHAR(50) NOT NULL,
    PRIMARY KEY (learner_state_id, learning_outcome_id),
    CONSTRAINT ck_learner_state_outcome_value CHECK (
        (prediction_status = 'PREDICTED' AND probability IS NOT NULL
            AND probability >= 0.0 AND probability <= 1.0)
        OR
        (prediction_status = 'INSUFFICIENT_PREDICTION' AND probability IS NULL)
    )
);

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

CREATE TABLE IF NOT EXISTS code_runner_submission_actions
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id   UUID NOT NULL REFERENCES code_submissions(id) ON DELETE CASCADE,
    attempt_id      UUID NOT NULL REFERENCES attempts(id) ON DELETE CASCADE,
    question_id     UUID NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,
    actor_id        UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action          VARCHAR(30) NOT NULL CHECK(action IN('REGRADE')),
    previous_status VARCHAR(30) NOT NULL,
    reason          VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_code_runner_submission_actions_submission
    ON code_runner_submission_actions(submission_id,created_at DESC);

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
    received_output      TEXT,
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
    matrix_id UUID REFERENCES exam_matrices(id) ON DELETE RESTRICT, matrix_version BIGINT, base_generation_job_id UUID,
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
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE, matrix_id UUID REFERENCES exam_matrices(id) ON DELETE RESTRICT, matrix_version BIGINT,
    quiz_id UUID REFERENCES quizzes(id) ON DELETE SET NULL,
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
    matrix_row_id UUID REFERENCES exam_matrix_rows(id) ON DELETE SET NULL,
    matrix_custom_learning_level_id UUID REFERENCES exam_matrix_custom_learning_levels(id) ON DELETE SET NULL,
    learning_outcome_id UUID REFERENCES learning_outcomes(id) ON DELETE SET NULL,
    source_question_id UUID REFERENCES questions(id) ON DELETE SET NULL, allocation_snapshot JSONB NOT NULL,
    qbank2_selection_mode VARCHAR(30),
    qbank2_content_group_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    qbank2_content_group_codes JSONB NOT NULL DEFAULT '[]'::jsonb,
    qbank2_example_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    qbank2_reference_snapshot JSONB,
    requested_asset_type VARCHAR(30) NOT NULL DEFAULT 'TEXT' CHECK (requested_asset_type IN ('TEXT','IMAGE','CODE_MCQ','CODE_RUNNER')),
    effective_asset_type VARCHAR(30) NOT NULL DEFAULT 'TEXT' CHECK (effective_asset_type IN ('TEXT','IMAGE','CODE_MCQ','CODE_RUNNER')),
    asset_fallback_reason VARCHAR(255),
    CONSTRAINT quiz3_question_provenance_qbank2_selection_mode_check
        CHECK(qbank2_selection_mode IS NULL OR qbank2_selection_mode IN('AUTO_SELECTED','RAG_FALLBACK','MANUAL_OVERRIDE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_quiz3_provenance_allocation ON quiz3_question_provenance(matrix_row_id,matrix_custom_learning_level_id,learning_outcome_id);

CREATE TABLE IF NOT EXISTS quiz3_qbank2_regeneration_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), assessment_id UUID NOT NULL REFERENCES quiz3_assessments(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES quiz3_assessment_variants(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE, requested_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    example_ids JSONB NOT NULL DEFAULT '[]'::jsonb, request_payload JSONB NOT NULL,
    base_question_version BIGINT NOT NULL CHECK(base_question_version >= 0),
    status VARCHAR(40) NOT NULL CHECK(status IN('QUEUED','RUNNING','APPLIED_TO_WORKING_DRAFT','FAILED','CANCELLED','TIMED_OUT')),
    phase VARCHAR(30) NOT NULL DEFAULT 'QUEUED', error_code VARCHAR(80), error_message TEXT, lease_until TIMESTAMPTZ,
    started_at TIMESTAMPTZ, completed_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_quiz3_qbank2_regeneration_active_job
    ON quiz3_qbank2_regeneration_jobs(question_id) WHERE status IN('QUEUED','RUNNING');
CREATE TABLE IF NOT EXISTS quiz3_qbank2_regeneration_drafts (
    question_id UUID PRIMARY KEY REFERENCES questions(id) ON DELETE CASCADE,
    assessment_id UUID NOT NULL REFERENCES quiz3_assessments(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES quiz3_assessment_variants(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES quiz3_qbank2_regeneration_jobs(id) ON DELETE CASCADE,
    base_question_version BIGINT NOT NULL CHECK(base_question_version >= 0), content TEXT NOT NULL,
    content_format VARCHAR(30) NOT NULL, explanation TEXT, explanation_format VARCHAR(30) NOT NULL,
    answers JSONB NOT NULL, content_group_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    content_group_codes JSONB NOT NULL DEFAULT '[]'::jsonb, example_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    reference_snapshot JSONB, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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

-- Quiz3 keeps its embedded-image ownership separate from the legacy Quiz2
-- assets.  PDF export reads this table even for questions without images, so
-- it must exist for every Quiz3 installation.
CREATE TABLE IF NOT EXISTS quiz3_rich_content_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    storage_key VARCHAR(1024) NOT NULL,
    media_type VARCHAR(255) NOT NULL,
    byte_size BIGINT NOT NULL CHECK (byte_size >= 0 AND byte_size <= 10485760),
    sha256 CHAR(64) NOT NULL,
    ai_artifact_id UUID REFERENCES quiz3_question_ai_artifacts(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_quiz3_rich_content_assets_question
    ON quiz3_rich_content_assets(question_id);
CREATE INDEX IF NOT EXISTS idx_quiz3_rich_content_assets_ai_artifact
    ON quiz3_rich_content_assets(ai_artifact_id) WHERE ai_artifact_id IS NOT NULL;

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
