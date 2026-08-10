package edu.ai.lms.core.coderunner.app.domain;

import edu.ai.lms.common.api.Roles;

import java.util.UUID;

public record CodeRunnerActor(UUID userId, Roles role) {
}
