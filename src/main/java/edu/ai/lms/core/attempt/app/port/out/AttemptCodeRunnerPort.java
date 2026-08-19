package edu.ai.lms.core.attempt.app.port.out;

import edu.ai.lms.core.attempt.app.domain.AttemptCodeRunnerQuestion;
import edu.ai.lms.core.attempt.app.domain.AttemptCodeSubmission;

import java.util.OptionalDouble;
import java.util.UUID;

public interface AttemptCodeRunnerPort {
    AttemptCodeRunnerQuestion getQuestion(UUID attemptId, UUID quizId, UUID questionId);
    UUID execute(UUID attemptId, UUID studentId, UUID quizId, UUID questionId,
                                  String sourceCode, String idempotencyKey, String mode);
    AttemptCodeSubmission getSubmission(UUID studentId, UUID submissionId, boolean hidesPrivateGrading);
    boolean isValidSubmit(UUID submissionId, UUID attemptId, UUID studentId, UUID questionId);
    boolean isTerminalSubmit(UUID submissionId, UUID attemptId, UUID studentId, UUID questionId);
    OptionalDouble findCompletedSubmitRatio(UUID submissionId, UUID attemptId, UUID studentId, UUID questionId);
    OptionalDouble findLatestCompletedSubmitRatio(UUID attemptId, UUID questionId);
    boolean hasPendingSubmissions(UUID attemptId);
}
