package edu.ai.lms.core.quiz.app.domain.questionbank;

import java.util.UUID;

public record CognitiveLevelOption(UUID id, CognitiveLevelCode code, String name, Integer sortOrder) {
}
