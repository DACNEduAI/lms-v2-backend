-- A student may have only one non-terminal attempt for a quiz. The service
-- checks first for a useful conflict message; this index closes the race
-- between concurrent start requests.
CREATE UNIQUE INDEX IF NOT EXISTS uq_attempts_student_quiz_active
    ON edu.attempts (student_id, quiz_id)
    WHERE status IN ('STARTED', 'FINALIZING');
