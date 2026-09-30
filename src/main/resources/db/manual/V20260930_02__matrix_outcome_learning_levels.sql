BEGIN;

DO $$
BEGIN
    IF to_regclass('edu.exam_matrix_outcome_cognitive_levels') IS NOT NULL
       AND to_regclass('edu.exam_matrix_outcome_learning_levels') IS NULL THEN
        ALTER TABLE edu.exam_matrix_outcome_cognitive_levels
            RENAME TO exam_matrix_outcome_learning_levels;
    END IF;

    IF to_regclass('edu.exam_matrix_outcome_learning_levels') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM information_schema.columns
           WHERE table_schema = 'edu'
             AND table_name = 'exam_matrix_outcome_learning_levels'
             AND column_name = 'custom_learning_level_id'
       ) THEN
        ALTER TABLE edu.exam_matrix_outcome_learning_levels
            ADD COLUMN custom_learning_level_id UUID;
    END IF;
END $$;

-- A row's existing CLL is authoritative only when every row in the LO column
-- uses the same one.  Ambiguous columns are deliberately left for a teacher
-- to select again instead of silently changing their asset coverage.
UPDATE edu.exam_matrix_outcome_learning_levels assignment
SET custom_learning_level_id = source.custom_learning_level_id
FROM (
    SELECT r.matrix_id, r.mapped_learning_outcome_id,
           (array_agg(c.custom_learning_level_id ORDER BY c.custom_learning_level_id))[1] AS custom_learning_level_id
    FROM edu.exam_matrix_rows r
    JOIN edu.exam_matrix_cells c ON c.row_id = r.id
    WHERE r.mapped_learning_outcome_id IS NOT NULL
    GROUP BY r.matrix_id, r.mapped_learning_outcome_id
    HAVING count(DISTINCT c.custom_learning_level_id) = 1
) source
WHERE assignment.matrix_id = source.matrix_id
  AND assignment.learning_outcome_id = source.mapped_learning_outcome_id
  AND assignment.custom_learning_level_id IS NULL;

-- For a legacy selection with no rows yet, retain it only if exactly one CLL
-- has the old Bloom-equivalent label.  This block is skipped on fresh schema.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'edu'
          AND table_name = 'exam_matrix_outcome_learning_levels'
          AND column_name = 'cognitive_level_code'
    ) THEN
        EXECUTE $migration$
            UPDATE edu.exam_matrix_outcome_learning_levels assignment
            SET custom_learning_level_id = source.custom_learning_level_id
            FROM (
                SELECT assignment.matrix_id, assignment.learning_outcome_id,
                       (array_agg(level.id ORDER BY level.id))[1] AS custom_learning_level_id
                FROM edu.exam_matrix_outcome_learning_levels assignment
                JOIN edu.exam_matrix_custom_learning_levels level
                  ON level.matrix_id = assignment.matrix_id
                WHERE assignment.custom_learning_level_id IS NULL
                  AND CASE lower(btrim(level.name))
                      WHEN 'remember' THEN 'REMEMBER'
                      WHEN 'recall' THEN 'REMEMBER'
                      WHEN 'recognize' THEN 'REMEMBER'
                      WHEN 'understand' THEN 'UNDERSTAND'
                      WHEN 'comprehend' THEN 'UNDERSTAND'
                      WHEN 'apply' THEN 'APPLY'
                      WHEN 'implement' THEN 'APPLY'
                      WHEN 'implementation' THEN 'APPLY'
                      WHEN 'analyze' THEN 'ANALYZE'
                      WHEN 'analyse' THEN 'ANALYZE'
                      WHEN 'analysis' THEN 'ANALYZE'
                      WHEN 'evaluate' THEN 'EVALUATE'
                      WHEN 'evaluation' THEN 'EVALUATE'
                      WHEN 'create' THEN 'CREATE'
                      WHEN 'creating' THEN 'CREATE'
                      WHEN 'synthesize' THEN 'CREATE'
                      WHEN 'design' THEN 'CREATE'
                  END = assignment.cognitive_level_code
                GROUP BY assignment.matrix_id, assignment.learning_outcome_id
                HAVING count(level.id) = 1
            ) source
            WHERE assignment.matrix_id = source.matrix_id
              AND assignment.learning_outcome_id = source.learning_outcome_id
              AND assignment.custom_learning_level_id IS NULL
        $migration$;
    END IF;
END $$;

DELETE FROM edu.exam_matrix_outcome_learning_levels
WHERE custom_learning_level_id IS NULL;

ALTER TABLE edu.exam_matrix_outcome_learning_levels
    DROP COLUMN IF EXISTS cognitive_level_code;

ALTER TABLE edu.exam_matrix_outcome_learning_levels
    ALTER COLUMN custom_learning_level_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_exam_matrix_outcome_learning_level_cll'
          AND conrelid = 'edu.exam_matrix_outcome_learning_levels'::regclass
    ) THEN
        ALTER TABLE edu.exam_matrix_outcome_learning_levels
            ADD CONSTRAINT fk_exam_matrix_outcome_learning_level_cll
            FOREIGN KEY (custom_learning_level_id)
            REFERENCES edu.exam_matrix_custom_learning_levels (id)
            ON DELETE CASCADE;
    END IF;
END $$;

COMMIT;
