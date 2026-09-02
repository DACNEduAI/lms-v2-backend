# Learner Model contract lock

This implementation follows the locked MVP contract from the source-aligned
plan.

- MCQ answers are persisted only at final attempt submission; draft/autosave
  behaviour is out of scope.
- The scorer returns grading facts and has no persistence side effect.
- `SubmitAttemptService` persists final MCQ snapshots in its transaction.
- Learner events will be introduced only after an attempt becomes `FINISHED`.
  A `FINALIZING` attempt therefore has snapshots but no learner event.
- A future learner event derives `course_id` from the persisted Quiz relation,
  never from a request path value.
- Code Runner evidence uses the attempt-selected submission snapshot. System
  failures are not learner-incorrect evidence.
- The existing FastAPI Quiz/RAG process remains separate from the future
  Learner Model worker, which will own only its PostgreSQL job processing.
