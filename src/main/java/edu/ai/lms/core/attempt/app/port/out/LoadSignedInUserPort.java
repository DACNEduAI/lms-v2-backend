package edu.ai.lms.core.attempt.app.port.out;

import edu.ai.lms.core.attempt.app.domain.CurrentActor;

import java.util.Optional;

public interface LoadSignedInUserPort {

    Optional<CurrentActor> getAuthenticatedUser();
}
