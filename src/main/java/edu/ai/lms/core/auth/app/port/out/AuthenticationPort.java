package edu.ai.lms.core.auth.app.port.out;

import org.springframework.security.core.Authentication;

/**
 * Port for authenticating users.
 * This port abstracts the authentication mechanism from the application layer.
 */
public interface AuthenticationPort {
    
    /**
     * Authenticates a user with the provided credentials.
     *
     * @param email    the user's email
     * @param password the user's password
     * @return the Authentication object containing the authenticated user's details
     * @throws org.springframework.security.core.AuthenticationException if authentication fails
     */
    Authentication authenticate(String email, String password);
}
