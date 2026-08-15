-- Normalize only the Quiz Generation parent and visual-child queues.
-- Run during a maintenance window after a verified, restorable database backup.
BEGIN;

DO $$
DECLARE
    parent_count BIGINT;
    child_count BIGINT;
BEGIN
    IF EXISTS (
        SELECT 1 FROM edu.quiz_generation_jobs
        WHERE status IN ('PENDING', 'PROCESSING')
    ) OR EXISTS (
        SELECT 1 FROM edu.question_image_generation_jobs
        WHERE status IN ('PENDING', 'PROCESSING')
    ) THEN
        RAISE EXCEPTION 'Refusing UTC migration while quiz-generation work is active';
    END IF;

    SELECT COUNT(*) INTO parent_count FROM edu.quiz_generation_jobs;
    IF parent_count <> 2 OR EXISTS (
        SELECT 1 FROM edu.quiz_generation_jobs
        WHERE status <> 'TIMED_OUT'
           OR phase <> 'TEXT_GENERATION'
           OR attempt_count <> 0
           OR error_code <> 'GENERATION_TIMEOUT'
    ) THEN
        RAISE EXCEPTION 'Unexpected quiz_generation_jobs history; audit timestamp origins before migration';
    END IF;

    SELECT COUNT(*) INTO child_count FROM edu.question_image_generation_jobs;
    IF child_count <> 0 THEN
        RAISE EXCEPTION 'Unexpected visual queue history; audit timestamp origins before migration';
    END IF;
END $$;

-- Parent rows were mixed-source: database NOW() fields are UTC; Java-written
-- scheduling/completion fields are Asia/Ho_Chi_Minh wall-clock values.
ALTER TABLE edu.quiz_generation_jobs
    ALTER COLUMN next_attempt_at TYPE TIMESTAMPTZ
        USING next_attempt_at AT TIME ZONE 'Asia/Ho_Chi_Minh',
    ALTER COLUMN lease_until TYPE TIMESTAMPTZ
        USING lease_until AT TIME ZONE 'Asia/Ho_Chi_Minh',
    ALTER COLUMN processing_at TYPE TIMESTAMPTZ
        USING processing_at AT TIME ZONE 'UTC',
    ALTER COLUMN completed_at TYPE TIMESTAMPTZ
        USING completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
        USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ
        USING updated_at AT TIME ZONE 'UTC';

-- The visual queue is verified empty above, so this preserves null values and
-- establishes timezone-aware columns for future visual work.
ALTER TABLE edu.question_image_generation_jobs
    ALTER COLUMN next_attempt_at TYPE TIMESTAMPTZ
        USING next_attempt_at AT TIME ZONE 'UTC',
    ALTER COLUMN lease_until TYPE TIMESTAMPTZ
        USING lease_until AT TIME ZONE 'UTC',
    ALTER COLUMN processing_at TYPE TIMESTAMPTZ
        USING processing_at AT TIME ZONE 'UTC',
    ALTER COLUMN completed_at TYPE TIMESTAMPTZ
        USING completed_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
        USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ
        USING updated_at AT TIME ZONE 'UTC';

COMMIT;
