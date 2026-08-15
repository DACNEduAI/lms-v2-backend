-- Remove the retired Quick Test flow from an existing database.
-- Run only during a maintenance window after a verified, restorable backup.
BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM edu.quiz_generation_jobs
        WHERE generation_mode = 'QUICK_TEST'
          AND status IN ('PENDING', 'PROCESSING')
    ) OR EXISTS (
        SELECT 1
        FROM edu.question_image_generation_jobs visual
        JOIN edu.quiz_generation_jobs parent ON parent.id = visual.quiz_generation_job_id
        WHERE parent.generation_mode = 'QUICK_TEST'
          AND visual.status IN ('PENDING', 'PROCESSING')
    ) THEN
        RAISE EXCEPTION 'Refusing Quick Test removal while generation work is active';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM edu.attempts attempt
        WHERE attempt.quiz_id IN (
            SELECT variant.quiz_id
            FROM edu.assessment_variants variant
            JOIN edu.assessments assessment ON assessment.id = variant.assessment_id
            WHERE assessment.generation_mode = 'QUICK_TEST'
            UNION
            SELECT job.quiz_id
            FROM edu.quiz_generation_jobs job
            WHERE job.generation_mode = 'QUICK_TEST'
              AND job.quiz_id IS NOT NULL
        )
    ) THEN
        RAISE EXCEPTION 'Refusing Quick Test removal because student attempts exist';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM edu.quiz_generation_jobs
        WHERE generation_mode <> 'QUICK_TEST'
          AND exam_matrix_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Refusing Quick Test removal because a Standard generation job has no Exam Matrix';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM edu.question_generation_provenance provenance
        JOIN edu.questions question ON question.id = provenance.question_id
        JOIN edu.quiz_generation_jobs job ON job.id = provenance.generation_job_id
        WHERE job.generation_mode = 'QUICK_TEST'
          AND question.quiz_id IS DISTINCT FROM job.quiz_id
    ) THEN
        RAISE EXCEPTION 'Refusing Quick Test removal because its question provenance is inconsistent';
    END IF;
END $$;

-- Deleting quizzes first cascades their questions, variants and generation
-- provenance so their parent jobs are no longer protected by a foreign key.
DELETE FROM edu.quizzes quiz
WHERE quiz.id IN (
    SELECT variant.quiz_id
    FROM edu.assessment_variants variant
    JOIN edu.assessments assessment ON assessment.id = variant.assessment_id
    WHERE assessment.generation_mode = 'QUICK_TEST'
    UNION
    SELECT job.quiz_id
    FROM edu.quiz_generation_jobs job
    WHERE job.generation_mode = 'QUICK_TEST'
      AND job.quiz_id IS NOT NULL
);

DELETE FROM edu.quiz_generation_jobs
WHERE generation_mode = 'QUICK_TEST';

DELETE FROM edu.assessments
WHERE generation_mode = 'QUICK_TEST';

ALTER TABLE edu.quiz_generation_jobs
    DROP CONSTRAINT IF EXISTS quiz_generation_jobs_matrix_for_standard_check,
    DROP CONSTRAINT IF EXISTS quiz_generation_jobs_generation_mode_check,
    DROP COLUMN generation_mode,
    ALTER COLUMN exam_matrix_id SET NOT NULL;

ALTER TABLE edu.assessments
    DROP CONSTRAINT IF EXISTS assessments_generation_mode_check,
    DROP COLUMN generation_mode;

COMMIT;
