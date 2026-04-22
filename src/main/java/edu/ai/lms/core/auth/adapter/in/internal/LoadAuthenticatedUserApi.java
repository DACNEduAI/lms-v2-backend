package edu.ai.lms.core.auth.adapter.in.internal;

import java.util.Optional;

import java.util.UUID;

import edu.ai.lms.core.auth.app.domain.UserAuth;

/**
 * Public API exposed by the Auth module for retrieving the currently authenticated user.
 * Other modules should depend on this interface to obtain authentication information.
 */
public interface LoadAuthenticatedUserApi {

    /**
     * Retrieves the currently authenticated user from the security context.
     *
     * @return an Optional containing the authenticated User, or empty if not authenticated
     */
    Optional<UserAuth> getAuthenticatedUser();

    /**
     * Retrieves the ID of the currently authenticated user.
     *
     * @return the UUID of the authenticated user
     */
    UUID getCurrentUserId();
}
