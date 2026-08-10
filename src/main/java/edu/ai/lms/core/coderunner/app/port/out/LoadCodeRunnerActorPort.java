package edu.ai.lms.core.coderunner.app.port.out;

import edu.ai.lms.core.coderunner.app.domain.CodeRunnerActor;

import java.util.Optional;

public interface LoadCodeRunnerActorPort {

    Optional<CodeRunnerActor> loadCurrentActor();
}
