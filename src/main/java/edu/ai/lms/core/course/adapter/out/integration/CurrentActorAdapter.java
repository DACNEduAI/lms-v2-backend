package edu.ai.lms.core.course.adapter.out.integration;

import java.util.Optional;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.api.CurrentPrincipalApi;
import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CurrentActorAdapter implements LoadSignedInUserPort {

    private final CurrentPrincipalApi currentPrincipalApi;

    @Override
    public Optional<CurrentActor> findAuthenticatedUser() {
        return currentPrincipalApi.currentPrincipal()
                .map(principal -> new CurrentActor(principal.userId(), principal.role()));
    }
}
