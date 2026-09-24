-- Matrix CLL asset allocations are LMS-only metadata for the current rollout.
-- Keep the existing Quiz3 -> ai_pipeline snapshot contract unchanged.
BEGIN;

CREATE TABLE IF NOT EXISTS edu.exam_matrix_custom_learning_level_asset_coverages
(
    custom_learning_level_id UUID        NOT NULL REFERENCES edu.exam_matrix_custom_learning_levels (id) ON DELETE CASCADE,
    asset_type               VARCHAR(30) NOT NULL CHECK (asset_type IN ('TEXT', 'IMAGE', 'CODE_MCQ', 'CODE_RUNNER')),
    percentage               INTEGER     NOT NULL CHECK (percentage BETWEEN 0 AND 100),
    PRIMARY KEY (custom_learning_level_id, asset_type)
);

INSERT INTO edu.exam_matrix_custom_learning_level_asset_coverages
    (custom_learning_level_id, asset_type, percentage)
SELECT level.id, allocation.asset_type, allocation.percentage
FROM edu.exam_matrix_custom_learning_levels level
CROSS JOIN (VALUES
    ('TEXT', 100),
    ('IMAGE', 0),
    ('CODE_MCQ', 0),
    ('CODE_RUNNER', 0)
) AS allocation(asset_type, percentage)
ON CONFLICT (custom_learning_level_id, asset_type) DO NOTHING;

COMMIT;
