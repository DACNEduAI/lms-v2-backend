# Code Runner P4 deployment and rollback

## Before rollout

1. Back up the production database and record the deployed application commit.
2. Apply `V20260820_03__code_runner_submission_regrade_audit.sql`. The migration is additive: it only creates the regrade audit table and its index.
3. Configure Judge0 credentials plus `JUDGE0_HEALTH_TIMEOUT`. Keep the existing CPU, wall-time, memory, output, thread and source-size limits enabled.
4. Point monitoring at `/actuator/health`. The `judge0` component reports Judge0 connectivity; `codeRunnerQueue` reports submissions older than five minutes and attempts left in `FINALIZING` longer than fifteen minutes.
5. Run a canary lifecycle: teacher verify, student RUN, student SUBMIT, finish to `FINALIZING`, then observe `FINISHED`.

## Rollback

1. Stop accepting new Code Runner submissions, but allow the current worker to drain its queue.
2. Roll the application back to the previous commit. Existing submission and contract data is backward compatible.
3. Keep `edu.code_runner_submission_actions` in place during rollback. It is additive and retaining it preserves the audit trail.
4. Only after a verified database backup, and only if the table must be removed, an operator may run `DROP TABLE edu.code_runner_submission_actions`. This destructive step is intentionally not automated.
5. Re-run the previous release's smoke tests and confirm no attempts remain in `FINALIZING`.

## Operational recovery

- A stale `PROCESSING` lease is reclaimed by the worker and test-result writes are idempotent.
- A lost completion event is repaired by the scheduled attempt finalizer.
- A teacher regrade keeps the originally selected submission, records the actor and reason, resets its durable results, and returns the attempt to `FINALIZING` until grading completes.
