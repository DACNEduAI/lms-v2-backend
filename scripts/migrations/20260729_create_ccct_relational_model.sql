BEGIN;

SET search_path TO edu;

CREATE TABLE ccct_profiles
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id        UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    name             VARCHAR(255) NOT NULL CHECK (btrim(name) <> ''),
    template_title   VARCHAR(255) NOT NULL CHECK (btrim(template_title) <> ''),
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    version          BIGINT       NOT NULL DEFAULT 0 CHECK (version >= 0),
    last_mode        VARCHAR(20)  NOT NULL
        CHECK (last_mode IN ('MARKDOWN', 'STRUCTURED')),
    markdown_content TEXT         NOT NULL DEFAULT '',
    created_by       UUID REFERENCES users (id) ON DELETE SET NULL,
    published_at     TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_ccct_profile_name_per_course
    ON ccct_profiles (course_id, lower(btrim(name)));
CREATE UNIQUE INDEX uq_ccct_one_published_per_course
    ON ccct_profiles (course_id)
    WHERE status = 'PUBLISHED';
CREATE INDEX idx_ccct_profiles_course_updated
    ON ccct_profiles (course_id, updated_at DESC);

CREATE TABLE ccct_chapters
(
    id                UUID PRIMARY KEY,
    profile_id        UUID         NOT NULL REFERENCES ccct_profiles (id) ON DELETE CASCADE,
    course_chapter_id UUID REFERENCES chapters (chapter_id) ON DELETE SET NULL,
    title             VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    order_index       INTEGER      NOT NULL CHECK (order_index >= 0),
    CONSTRAINT uq_ccct_chapter_order UNIQUE (profile_id, order_index)
);

CREATE INDEX idx_ccct_chapters_profile
    ON ccct_chapters (profile_id);
CREATE INDEX idx_ccct_chapters_course_chapter
    ON ccct_chapters (course_chapter_id);

CREATE TABLE ccct_topics
(
    id          UUID PRIMARY KEY,
    chapter_id  UUID         NOT NULL REFERENCES ccct_chapters (id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    order_index INTEGER      NOT NULL CHECK (order_index >= 0),
    CONSTRAINT uq_ccct_topic_order UNIQUE (chapter_id, order_index)
);

CREATE INDEX idx_ccct_topics_chapter
    ON ccct_topics (chapter_id);

CREATE TABLE ccct_items
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

CREATE INDEX idx_ccct_items_topic_status
    ON ccct_items (topic_id, coverage_status);

ALTER TABLE exam_matrices
    ADD COLUMN ccct_profile_id UUID,
    ADD CONSTRAINT exam_matrices_ccct_profile_id_fkey
        FOREIGN KEY (ccct_profile_id) REFERENCES ccct_profiles (id) ON DELETE RESTRICT;

ALTER TABLE exam_matrix_rows
    ADD COLUMN ccct_item_id UUID,
    ADD CONSTRAINT exam_matrix_rows_ccct_item_id_fkey
        FOREIGN KEY (ccct_item_id) REFERENCES ccct_items (id) ON DELETE RESTRICT;

ALTER TABLE exam_matrix_rows
    DROP CONSTRAINT exam_matrix_rows_chapter_id_fkey,
    ALTER COLUMN chapter_id DROP NOT NULL,
    ADD CONSTRAINT exam_matrix_rows_chapter_id_fkey
        FOREIGN KEY (chapter_id) REFERENCES chapters (chapter_id) ON DELETE SET NULL;

CREATE INDEX idx_exam_matrices_ccct_profile
    ON exam_matrices (ccct_profile_id);
CREATE INDEX idx_exam_matrix_rows_ccct_item
    ON exam_matrix_rows (ccct_item_id);
CREATE UNIQUE INDEX uq_exam_matrix_ccct_item
    ON exam_matrix_rows (matrix_id, ccct_item_id)
    WHERE ccct_item_id IS NOT NULL;

COMMIT;
