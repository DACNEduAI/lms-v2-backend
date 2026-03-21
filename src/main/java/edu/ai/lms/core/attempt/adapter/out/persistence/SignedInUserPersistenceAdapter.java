package edu.ai.lms.core.attempt.adapter.out.persistence;

import edu.ai.lms.core.attempt.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.auth.adapter.in.internal.LoadAuthenticatedUserApi;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class SignedInUserPersistenceAdapter implements LoadSignedInUserPort {

    private final LoadAuthenticatedUserApi loadAuthenticatedUserApi;

    @Override
    public Optional<UserAuth> getAuthenticatedUser() {
        return loadAuthenticatedUserApi.getAuthenticatedUser();
    }
}
