package edu.ai.lms.core.coderunner.adapter.out.integration;

import edu.ai.lms.core.auth.api.CurrentPrincipalApi;
import edu.ai.lms.core.coderunner.app.domain.CodeRunnerActor;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerActorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class CurrentCodeRunnerActorAdapter implements LoadCodeRunnerActorPort {

    private final CurrentPrincipalApi currentPrincipalApi;

    @Override
    public Optional<CodeRunnerActor> loadCurrentActor() {
        return currentPrincipalApi.currentPrincipal()
                .map(principal -> new CodeRunnerActor(principal.userId(), principal.role()));
    }
}
