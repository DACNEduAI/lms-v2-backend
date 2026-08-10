package edu.ai.lms.core.coderunner.app.domain;

import java.util.UUID;

public record CodeRunnerQuestionContext(
        UUID questionId,
        UUID quizId,
        UUID courseId,
        UUID teacherId,
        String questionType,
        String questionReviewStatus,
        String quizLifecycleStatus,
        long questionVersion) {
}
