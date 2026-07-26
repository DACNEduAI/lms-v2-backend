package edu.ai.lms.core.quiz.app.domain;

import java.util.UUID;

public record QuestionLearningOutcomeLink(UUID questionId, UUID learningOutcomeId) {
}
