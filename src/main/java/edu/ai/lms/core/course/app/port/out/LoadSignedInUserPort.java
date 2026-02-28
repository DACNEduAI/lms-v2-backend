package edu.ai.lms.core.course.app.port.out;

import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.common.Roles;

public interface LoadSignedInUserPort {
    
    /**
     * Represents the authenticated user's information needed for course operations.
     */
    record SignedInUser(UUID id, Roles role) {}
    
    /**
     * Finds the currently authenticated user (either Teacher or Student).
     * @return Optional containing the signed-in user's ID and role, or empty if not authenticated.
     */
    Optional<SignedInUser> findAuthenticatedUser();
}
