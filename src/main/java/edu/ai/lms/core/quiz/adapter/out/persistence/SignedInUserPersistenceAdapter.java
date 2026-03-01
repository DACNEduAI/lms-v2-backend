package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.adapter.in.internal.LoadAuthenticatedUserApi;
import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SignedInUserPersistenceAdapter implements LoadSignedInUserPort {

    private final LoadAuthenticatedUserApi loadAuthenticatedUserApi;

    @Override
    public Optional<User> findAuthenticatedUser() {
        return loadAuthenticatedUserApi.getAuthenticatedUser();
    }
}
