package edu.ai.lms.core.quiz.app.service.assessment;

import java.util.List;

/**
 * Server-owned context boundary for question refinement. It intentionally contains
 * only locked assessment metadata in this iteration; selected materials and
 * citation/style controls can be added without changing the teacher chat contract.
 */
public record QuestionRefinementContext(
        String matrixRow,
        String chapter,
        String topic,
        String customLearningLevelCode,
        String customLearningLevelName,
        List<String> learningOutcomes,
        String cognitiveLevelCode,
        String cognitiveLevelName) {
}
