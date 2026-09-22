-- Run once with the backend stopped, after the Quiz3 rich-content migration.
-- Historical db/manual scripts remain history; do not replay them after this one.
BEGIN;
SET LOCAL search_path TO edu, public;
SET LOCAL lock_timeout = '10s';

-- Reject live work before copying or dropping its state.
DO $$
DECLARE
    pending_count bigint;
    job record;
BEGIN
    FOR job IN SELECT * FROM (VALUES
        ('quiz_generation_jobs', 'status', ARRAY['PENDING','PROCESSING']),
        ('question_image_generation_jobs', 'status', ARRAY['PENDING','PROCESSING']),
        ('assessment_generation_batches', 'status', ARRAY['QUEUED','PROCESSING']),
        ('assessment_generation_batch_items', 'status', ARRAY['QUEUED','PROCESSING']),
        ('question_ai_jobs', 'status', ARRAY['QUEUED','RUNNING','NEEDS_CLARIFICATION'])
    ) AS jobs(table_name, status_column, active_statuses) LOOP
        IF to_regclass('edu.' || job.table_name) IS NOT NULL THEN
            EXECUTE format('SELECT count(*) FROM edu.%I WHERE %I = ANY ($1)',
                job.table_name, job.status_column)
                INTO pending_count USING job.active_statuses;
            IF pending_count > 0 THEN
                RAISE EXCEPTION 'Active legacy jobs in edu.%: %', job.table_name, pending_count;
            END IF;
        END IF;
    END LOOP;
END $$;

-- No Quiz3 table may depend on a table in the retirement set.
DO $$
DECLARE
    dependency text;
BEGIN
    SELECT format('%s -> %s (%s)', conrelid::regclass, confrelid::regclass, conname)
    INTO dependency
    FROM pg_constraint
    WHERE contype = 'f'
      AND connamespace = 'edu'::regnamespace
      AND conrelid IN (SELECT oid FROM pg_class
                       WHERE relnamespace = 'edu'::regnamespace
                         AND relname LIKE 'quiz3_%')
      AND confrelid = ANY (ARRAY[
          to_regclass('edu.assessments'), to_regclass('edu.assessment_variants'),
          to_regclass('edu.quiz_generation_jobs'), to_regclass('edu.assessment_generation_batches'),
          to_regclass('edu.assessment_generation_batch_items'), to_regclass('edu.assessment_webhook_events'),
          to_regclass('edu.question_drafts'), to_regclass('edu.question_draft_answers'),
          to_regclass('edu.question_draft_learning_outcomes'), to_regclass('edu.question_bank_questions'),
          to_regclass('edu.question_bank_answers'), to_regclass('edu.question_bank_question_learning_outcomes'),
          to_regclass('edu.question_bank_allocations'), to_regclass('edu.question_ai_working_drafts'),
          to_regclass('edu.question_ai_jobs'), to_regclass('edu.question_ai_job_steps'),
          to_regclass('edu.question_ai_artifacts'), to_regclass('edu.question_ai_artifact_revisions'),
          to_regclass('edu.question_ai_change_sets'), to_regclass('edu.rich_content_assets'),
          to_regclass('edu.question_generation_provenance'), to_regclass('edu.question_images'),
          to_regclass('edu.question_image_generation_jobs'), to_regclass('edu.exam_matrices'),
          to_regclass('edu.exam_matrix_rows'), to_regclass('edu.exam_matrix_row_learning_outcomes'),
          to_regclass('edu.custom_learning_levels'), to_regclass('edu.custom_learning_level_learning_outcomes'),
          to_regclass('edu.exam_matrix_row_learning_level_counts')
      ]::regclass[])
    LIMIT 1;
    IF dependency IS NOT NULL THEN
        RAISE EXCEPTION 'Quiz3 still depends on retired table: %', dependency;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS edu.quiz_question_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES edu.questions(id) ON DELETE CASCADE,
    source_type VARCHAR(20) NOT NULL DEFAULT 'UPLOAD'
        CHECK (source_type IN ('AUTO', 'LATEX', 'SVG', 'UPLOAD')),
    source_text TEXT, generation_brief TEXT, alt_text TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'READY'
        CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')),
    image_name VARCHAR(255), storage_key TEXT, url TEXT, media_type VARCHAR(255),
    content_hash VARCHAR(64), byte_size BIGINT, error_code VARCHAR(100), error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_quiz_question_media_question_id
    ON edu.quiz_question_media(question_id);

DO $$
DECLARE missing_count bigint;
BEGIN
    IF to_regclass('edu.question_images') IS NOT NULL THEN
        INSERT INTO edu.quiz_question_media (
            id, question_id, source_type, source_text, generation_brief, alt_text,
            status, image_name, storage_key, url, media_type, content_hash, byte_size,
            error_code, error_message, created_at, updated_at)
        SELECT id, question_id, COALESCE(source_type, 'UPLOAD'), source_text,
            generation_brief, alt_text, status, image_name, storage_key, url,
            media_type, content_hash, byte_size, error_code, error_message,
            created_at, updated_at
        FROM edu.question_images
        WHERE true
        ON CONFLICT (id) DO NOTHING;
        SELECT count(*) INTO missing_count
        FROM edu.question_images old
        LEFT JOIN edu.quiz_question_media media ON media.id = old.id
          AND media.question_id = old.question_id
        WHERE media.id IS NULL;
        IF missing_count > 0 THEN
            RAISE EXCEPTION 'Quiz media copy mismatch: % old images missing', missing_count;
        END IF;
    END IF;
END $$;

DO $$
DECLARE missing_count bigint;
BEGIN
    IF to_regclass('edu.rich_content_assets') IS NOT NULL THEN
        IF to_regclass('edu.quiz3_rich_content_assets') IS NULL THEN
            RAISE EXCEPTION 'Apply Quiz3 rich-content migration before retirement';
        END IF;
        INSERT INTO edu.quiz3_rich_content_assets (
            id, question_id, storage_key, media_type, byte_size, sha256,
            ai_artifact_id, created_at, updated_at)
        SELECT id, question_id, storage_key, media_type, byte_size, sha256,
            NULL, created_at, updated_at
        FROM edu.rich_content_assets
        WHERE question_id IS NOT NULL
        ON CONFLICT (id) DO NOTHING;
        SELECT count(*) INTO missing_count
        FROM edu.rich_content_assets old
        LEFT JOIN edu.quiz3_rich_content_assets asset ON asset.id = old.id
          AND asset.question_id = old.question_id
          AND asset.storage_key = old.storage_key
        WHERE old.question_id IS NOT NULL AND asset.id IS NULL;
        IF missing_count > 0 THEN
            RAISE EXCEPTION 'Quiz3 rich-content copy mismatch: % question assets missing', missing_count;
        END IF;
    END IF;
END $$;

-- Remove known references from retained question snapshots. Missing historical
-- columns are tolerated; unknown retained dependencies still block the drops.
ALTER TABLE edu.questions
    DROP COLUMN IF EXISTS source_bank_question_id,
    DROP COLUMN IF EXISTS matrix_row_id,
    DROP COLUMN IF EXISTS custom_learning_level_id;

-- One RESTRICT statement lets PostgreSQL resolve FKs within this exact 29-table
-- set, while refusing any dependency from a retained table.
DROP TABLE IF EXISTS
    edu.question_ai_change_sets, edu.question_ai_artifact_revisions,
    edu.question_ai_artifacts, edu.question_ai_job_steps,
    edu.question_ai_working_drafts, edu.question_ai_jobs,
    edu.question_draft_answers, edu.question_draft_learning_outcomes,
    edu.question_bank_answers, edu.question_bank_question_learning_outcomes,
    edu.question_bank_allocations, edu.assessment_webhook_events,
    edu.assessment_generation_batch_items, edu.question_image_generation_jobs,
    edu.question_generation_provenance, edu.assessment_variants,
    edu.question_drafts, edu.question_bank_questions,
    edu.assessment_generation_batches, edu.rich_content_assets,
    edu.question_images, edu.quiz_generation_jobs,
    edu.exam_matrix_row_learning_level_counts,
    edu.custom_learning_level_learning_outcomes,
    edu.exam_matrix_row_learning_outcomes, edu.custom_learning_levels,
    edu.exam_matrix_rows, edu.assessments, edu.exam_matrices
    RESTRICT;
COMMIT;
