package edu.ai.lms.core.history.adapter.out.persistence;

import edu.ai.lms.core.auth.adapter.in.internal.LoadAuthenticatedUserApi;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.history.app.port.out.LoadCurrentUserPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HistoryCurrentUserAdapter implements LoadCurrentUserPort {

    private final LoadAuthenticatedUserApi loadAuthenticatedUserApi;

    @Override
    public Optional<UUID> getCurrentUserId() {
        return loadAuthenticatedUserApi.getAuthenticatedUser().map(UserAuth::getId);
    }
}