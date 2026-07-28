package edu.ai.lms.core.course.app.port.out;

import java.util.Optional;

import edu.ai.lms.core.course.app.domain.CurrentActor;

public interface LoadSignedInUserPort {

    /**
     * Finds the currently authenticated user.
     * @return Optional containing the authenticated User, or empty if not authenticated.
     */
    Optional<CurrentActor> findAuthenticatedUser();
}
