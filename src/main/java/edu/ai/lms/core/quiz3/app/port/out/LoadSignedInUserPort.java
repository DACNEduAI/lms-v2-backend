package edu.ai.lms.core.quiz3.app.port.out;

import java.util.Optional;

import edu.ai.lms.core.quiz3.app.domain.CurrentActor;

public interface LoadSignedInUserPort {

    /**
     * Finds the currently authenticated user.
     * @return Optional containing the authenticated User, or empty if not authenticated.
     */
    Optional<CurrentActor> getAuthenticatedUser();
}
