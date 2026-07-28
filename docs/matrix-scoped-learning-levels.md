# Matrix-scoped learning levels

## Ownership

- A custom learning level belongs to exactly one exam matrix.
- Level codes and names are unique within that matrix.
- Deleting a matrix cascades its levels, rows, mappings, and question counts.

## Materialized checkbox mappings

`exam_matrix_row_learning_outcomes` contains one record for every outcome
assigned to the row's chapter. `custom_learning_level_learning_outcomes`
contains one record for every course outcome and every level in the matrix.

Both junctions use `is_selected` for checkbox state. Normal checkbox edits
update this field; they do not delete and recreate the relationship.

Chapter-outcome synchronization is owned by the Java backend. The chapter
persistence adapter inserts unchecked row/outcome candidates after assigning
an outcome to a chapter and removes those candidates after unassigning it.
Direct SQL writers must perform the equivalent synchronization themselves.

The API intentionally continues to expose only selected IDs:

- `ExamMatrixRowResponse.learningOutcomeIds`
- `CustomLearningLevelResponse.learningOutcomeIds`

## Completeness

A complete matrix has at least three custom learning levels, no empty level,
coverage for every course learning outcome, and at least one selected outcome
in every matrix row. A newly created course outcome is inserted into all
existing matrix-level junctions with `is_selected = false`, making those
matrices incomplete until a teacher assigns it.

Incomplete matrices remain readable but cannot be used for quiz generation or
saved as an incomplete matrix configuration.

## Dormant counts

A row-level question count becomes dormant when its level no longer shares a
selected outcome with the row. Dormant counts remain stored, are returned with
`active = false`, and are excluded from totals and generation. Restoring an
eligible mapping reactivates the original count.

## Deployment

Fresh databases use `src/main/resources/schema.sql`. Existing PostgreSQL
databases must run
`scripts/migrations/20260728_matrix_scoped_learning_levels.sql` before the new
backend is deployed. The migration clones legacy course-scoped levels for each
matrix, remaps counts, materializes false candidate pairs, and then applies the
new ownership constraints. Databases that previously ran a trigger-enabled
version of that migration must also run
`scripts/migrations/20260729_remove_chapter_outcome_sync_triggers.sql`.
