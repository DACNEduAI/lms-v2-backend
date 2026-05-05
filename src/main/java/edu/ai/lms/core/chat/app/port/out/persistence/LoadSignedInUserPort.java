package edu.ai.lms.core.chat.app.port.out.persistence;

import java.util.Optional;

import edu.ai.lms.core.auth.app.domain.UserAuth;

public interface LoadSignedInUserPort {
    Optional<UserAuth> getAuthenticatedUser();
}
