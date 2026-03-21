package edu.ai.lms.core.attempt.app.port.out;

import edu.ai.lms.core.attempt.app.domain.Attempt;

public interface SaveAttemptPort {

    Attempt save(Attempt attempt);
}
