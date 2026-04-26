package edu.ai.lms.core.material.app.port.out.persistence;

import java.util.Optional;
import edu.ai.lms.core.auth.app.domain.UserAuth;

/**
 * Port to load the currently signed-in user's information.
 * Used for tracking activity logs.
 */
public interface LoadSignedInUserPort {
    Optional<UserAuth> getAuthenticatedUser();
}
