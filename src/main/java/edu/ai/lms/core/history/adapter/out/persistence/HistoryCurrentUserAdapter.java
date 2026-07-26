package edu.ai.lms.core.history.adapter.out.persistence;

import edu.ai.lms.core.auth.api.CurrentPrincipalApi;
import edu.ai.lms.core.history.app.port.out.LoadCurrentUserPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HistoryCurrentUserAdapter implements LoadCurrentUserPort {

    private final CurrentPrincipalApi currentPrincipalApi;

    @Override
    public Optional<UUID> getCurrentUserId() {
        return currentPrincipalApi.currentPrincipal().map(principal -> principal.userId());
    }
}
