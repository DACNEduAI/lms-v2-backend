package edu.ai.lms.core.quiz3.api;

import java.util.UUID;

public record CodeRunnerQuestionContext(
        UUID questionId,
        UUID quizId,
        UUID courseId,
        UUID teacherId,
        String questionType,
        String questionReviewStatus,
        String quizLifecycleStatus,
        long questionVersion,
        String draftContractJson) {
}
