# Question Bank QB-0 decisions

> Baseline: `feature/ccct-api` at `1b67b1f`<br>
> Implementation branch: `feature/question-bank-foundation`<br>
> Date: 2026-08-02

This record freezes only decisions required to implement the QB-1 data and persistence foundation.

## Accepted decisions

1. `question_bank_items` is a reusable master. `questions` remains the independent snapshot owned by one Quiz. No live M:N Question sharing is introduced.
2. Editing a `READY_FOR_USE` Bank Item will return that same item to `DRAFT` in the MVP. A revision table is deferred.
3. Question Bank MVP is text-only MCQ. Existing Quiz images remain supported, but copying image binaries across Bank/Quiz ownership boundaries is deferred until an explicit storage-copy policy is implemented.
4. `display` remains a legacy delivery/visibility flag during migration. `lifecycle_status` is the new business state.
5. Backfill treats `display=true` as legacy `PUBLISHED` for compatibility only. It does not claim the new review workflow happened.
6. A hidden Quiz linked to a completed AI generation job is backfilled as `REVIEW_PENDING`; other hidden legacy quizzes become `DRAFT`.
7. Existing non-AI questions are backfilled as `APPROVED` so current quizzes keep working. Questions owned by legacy AI-generated quizzes become `PENDING_REVIEW`.
8. AI provenance keeps mutable planning IDs plus an immutable JSON allocation snapshot. Deleting Matrix/CCCT/level data must not cascade-delete a Quiz Question or its historical snapshot.
9. Course, Chapter, CCCT and LO consistency remains application validation. PostgreSQL foreign keys protect existence, while QB-2 will enforce same-Course ownership. Deleting a Chapter clears the Bank mapping instead of deleting the Bank Item; a Ready Item with missing required metadata must be resolved before further reuse.
10. `version` columns are added in QB-1. Compare-and-swap operations are introduced per use case in QB-2/QB-4 rather than changing every legacy CRUD endpoint silently.

## Explicitly deferred

- Question Bank HTTP API and validation;
- Ready/Archive transitions;
- Published Quiz mutation guards;
- deep clone;
- per-question review endpoints;
- saving provenance during callback;
- Bank examples in AI requests;
- image copy/object lifecycle;
- migration of production data without a dry-run report.

## Migration safety note

Run `scripts/migrations/20260802_question_bank_foundation.sql` on a database copy first and capture counts grouped by:

```sql
SELECT display, lifecycle_status, source_type, COUNT(*)
FROM edu.quizzes
GROUP BY display, lifecycle_status, source_type;
```

Any visible legacy Quiz that should remain editable must be identified before production rollout and assigned an explicit lifecycle according to the product owner's decision.
