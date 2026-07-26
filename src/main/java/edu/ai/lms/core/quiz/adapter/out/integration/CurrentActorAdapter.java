package edu.ai.lms.core.quiz.adapter.out.integration;

import java.util.Optional;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.api.CurrentPrincipalApi;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

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
