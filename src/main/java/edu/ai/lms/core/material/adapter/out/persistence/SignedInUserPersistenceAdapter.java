package edu.ai.lms.core.material.adapter.out.persistence;

import java.util.Optional;
import org.springframework.stereotype.Component;
import edu.ai.lms.core.auth.adapter.in.internal.LoadAuthenticatedUserApi;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.material.app.port.out.persistence.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Component("materialSignedInUserPersistenceAdapter")
@RequiredArgsConstructor
class SignedInUserPersistenceAdapter implements LoadSignedInUserPort {

    private final LoadAuthenticatedUserApi loadAuthenticatedUserApi;

    @Override
    public Optional<UserAuth> getAuthenticatedUser() {
        return loadAuthenticatedUserApi.getAuthenticatedUser();
    }
}
