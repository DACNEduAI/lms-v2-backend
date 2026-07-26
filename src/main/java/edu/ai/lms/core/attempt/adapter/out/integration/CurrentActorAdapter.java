package edu.ai.lms.core.attempt.adapter.out.integration;

import edu.ai.lms.core.attempt.app.domain.CurrentActor;
import edu.ai.lms.core.attempt.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.auth.api.CurrentPrincipalApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class CurrentActorAdapter implements LoadSignedInUserPort {

    private final CurrentPrincipalApi currentPrincipalApi;

    @Override
    public Optional<CurrentActor> getAuthenticatedUser() {
        return currentPrincipalApi.currentPrincipal()
                .map(principal -> new CurrentActor(principal.userId(), principal.role()));
    }
}
