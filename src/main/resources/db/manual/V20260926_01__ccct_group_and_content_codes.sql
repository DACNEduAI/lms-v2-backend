BEGIN;

ALTER TABLE edu.ccct_topics ADD COLUMN IF NOT EXISTS profile_id UUID;
UPDATE edu.ccct_topics t
SET profile_id = c.profile_id
FROM edu.ccct_chapters c
WHERE c.id = t.chapter_id AND t.profile_id IS NULL;

WITH ranked AS (
    SELECT t.id, row_number() OVER (
        PARTITION BY c.profile_id, t.group_code
        ORDER BY c.order_index, t.order_index, t.id
    ) AS occurrence
    FROM edu.ccct_topics t
    JOIN edu.ccct_chapters c ON c.id = t.chapter_id
)
UPDATE edu.ccct_topics t
SET group_code = 'g-' || replace(t.id::text, '-', '')
FROM ranked r
WHERE r.id = t.id AND r.occurrence > 1;

ALTER TABLE edu.ccct_topics ALTER COLUMN profile_id SET NOT NULL;
ALTER TABLE edu.ccct_topics DROP CONSTRAINT IF EXISTS uq_ccct_topic_group_code;
ALTER TABLE edu.ccct_topics ADD CONSTRAINT uq_ccct_topic_group_code UNIQUE (profile_id, group_code);

ALTER TABLE edu.ccct_items ADD COLUMN IF NOT EXISTS content_code VARCHAR(128);
WITH numbered AS (
    SELECT i.id, t.group_code,
           row_number() OVER (PARTITION BY i.topic_id ORDER BY i.order_index, i.id) AS item_number
    FROM edu.ccct_items i
    JOIN edu.ccct_topics t ON t.id = i.topic_id
)
UPDATE edu.ccct_items i
SET content_code = n.group_code || '.item-' || n.item_number
FROM numbered n
WHERE n.id = i.id AND i.content_code IS NULL;

ALTER TABLE edu.ccct_items ALTER COLUMN content_code SET NOT NULL;
ALTER TABLE edu.ccct_items DROP CONSTRAINT IF EXISTS ck_ccct_item_content_code;
ALTER TABLE edu.ccct_items ADD CONSTRAINT ck_ccct_item_content_code
    CHECK (content_code ~ '^[a-z0-9]+([._-][a-z0-9]+)*$');
ALTER TABLE edu.ccct_items DROP CONSTRAINT IF EXISTS uq_ccct_item_code;
ALTER TABLE edu.ccct_items ADD CONSTRAINT uq_ccct_item_code UNIQUE (topic_id, content_code);

CREATE TABLE IF NOT EXISTS edu.exam_matrix_outcome_cognitive_levels
(
    matrix_id           UUID         NOT NULL REFERENCES edu.exam_matrices (id) ON DELETE CASCADE,
    learning_outcome_id UUID         NOT NULL REFERENCES edu.learning_outcomes (id) ON DELETE CASCADE,
    cognitive_level_code VARCHAR(30) NOT NULL CHECK
        (cognitive_level_code IN ('REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE', 'EVALUATE', 'CREATE')),
    PRIMARY KEY (matrix_id, learning_outcome_id)
);

INSERT INTO edu.exam_matrix_outcome_cognitive_levels
    (matrix_id, learning_outcome_id, cognitive_level_code)
SELECT DISTINCT ON (r.matrix_id, r.mapped_learning_outcome_id)
       r.matrix_id, r.mapped_learning_outcome_id,
       CASE lower(btrim(l.name))
           WHEN 'remember' THEN 'REMEMBER'
           WHEN 'understand' THEN 'UNDERSTAND'
           WHEN 'apply' THEN 'APPLY'
           WHEN 'implement' THEN 'APPLY'
           WHEN 'implementation' THEN 'APPLY'
           WHEN 'analyze' THEN 'ANALYZE'
           WHEN 'analyse' THEN 'ANALYZE'
           WHEN 'evaluate' THEN 'EVALUATE'
           WHEN 'create' THEN 'CREATE'
       END
FROM edu.exam_matrix_rows r
JOIN edu.exam_matrix_cells c ON c.row_id = r.id
JOIN edu.exam_matrix_custom_learning_levels l ON l.id = c.custom_learning_level_id
WHERE r.mapped_learning_outcome_id IS NOT NULL
  AND lower(btrim(l.name)) IN (
      'remember', 'understand', 'apply', 'implement', 'implementation',
      'analyze', 'analyse', 'evaluate', 'create'
  )
ORDER BY r.matrix_id, r.mapped_learning_outcome_id, r.sort_order, r.id
ON CONFLICT (matrix_id, learning_outcome_id) DO NOTHING;

UPDATE edu.exam_matrices m
SET version = version + 1, updated_at = CURRENT_TIMESTAMP
WHERE EXISTS (
    SELECT 1 FROM edu.exam_matrix_outcome_cognitive_levels l WHERE l.matrix_id = m.id
);

UPDATE edu.ccct_profiles p
SET version = version + 1, updated_at = CURRENT_TIMESTAMP
WHERE EXISTS (
    SELECT 1 FROM edu.ccct_chapters c WHERE c.profile_id = p.id
);

COMMIT;
