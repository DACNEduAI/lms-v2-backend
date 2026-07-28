package edu.ai.lms.core.notification.adapter.out.integration;

import edu.ai.lms.core.auth.api.CurrentPrincipalApi;
import edu.ai.lms.core.notification.app.port.out.LoadCurrentUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("notificationCurrentUserAdapter")
@RequiredArgsConstructor
class CurrentUserAdapter implements LoadCurrentUserPort {

    private final CurrentPrincipalApi currentPrincipalApi;

    @Override
    public Optional<UUID> getCurrentUserId() {
        return currentPrincipalApi.currentPrincipal().map(principal -> principal.userId());
    }
}
