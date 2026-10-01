BEGIN;

ALTER TABLE edu.learner_state_outcomes
    ADD COLUMN IF NOT EXISTS akt_pykt_probability DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS akt_pykt_prediction_status VARCHAR(50);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_learner_state_outcome_akt_pykt_value'
          AND conrelid = 'edu.learner_state_outcomes'::regclass
    ) THEN
        ALTER TABLE edu.learner_state_outcomes
            ADD CONSTRAINT ck_learner_state_outcome_akt_pykt_value CHECK (
                (akt_pykt_prediction_status IS NULL AND akt_pykt_probability IS NULL)
                OR (akt_pykt_prediction_status = 'PREDICTED' AND akt_pykt_probability IS NOT NULL
                    AND akt_pykt_probability >= 0.0 AND akt_pykt_probability <= 1.0)
                OR (akt_pykt_prediction_status IN ('INSUFFICIENT_HISTORY', 'INSUFFICIENT_OUTCOME_HISTORY')
                    AND akt_pykt_probability IS NULL)
            );
    END IF;
END $$;

COMMIT;
