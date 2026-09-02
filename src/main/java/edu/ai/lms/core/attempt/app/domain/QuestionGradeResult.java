package edu.ai.lms.core.attempt.app.domain;

import java.util.UUID;

/**
 * Immutable per-question grading fact produced by {@link
 * edu.ai.lms.core.attempt.app.service.AttemptScoringService}.
 *
 * <p>It deliberately contains no persistence behaviour. The submit
 * orchestrator decides which grading facts must be stored as finalized
 * learner evidence.</p>
 */
public record QuestionGradeResult(
        UUID questionId,
        QuestionAnswerKey.QuestionType questionType,
        Long selectedAnswerNumber,
        ResponseStatus responseStatus,
        Boolean isCorrectSnapshot,
        Double scoreRatio
) {
    public enum ResponseStatus {
        ANSWERED,
        SKIPPED
    }

    public boolean isMultipleChoice() {
        return questionType == QuestionAnswerKey.QuestionType.MULTIPLE_CHOICE;
    }
}
