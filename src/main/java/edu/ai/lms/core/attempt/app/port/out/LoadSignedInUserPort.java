package edu.ai.lms.core.attempt.app.port.out;

import edu.ai.lms.core.auth.app.domain.UserAuth;

import java.util.Optional;

public interface LoadSignedInUserPort {

    Optional<UserAuth> getAuthenticatedUser();
}
