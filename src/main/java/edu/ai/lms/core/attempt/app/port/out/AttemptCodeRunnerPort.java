package edu.ai.lms.core.attempt.app.port.out;

import edu.ai.lms.core.attempt.app.domain.AttemptCodeRunnerQuestion;
import edu.ai.lms.core.attempt.app.domain.AttemptCodeSubmission;

import java.util.OptionalDouble;
import java.util.UUID;

public interface AttemptCodeRunnerPort {
    AttemptCodeRunnerQuestion getQuestion(UUID quizId, UUID questionId);
    AttemptCodeSubmission execute(UUID attemptId, UUID studentId, UUID quizId, UUID questionId,
                                  String sourceCode, String idempotencyKey, String mode);
    OptionalDouble findCompletedSubmitRatio(UUID submissionId, UUID attemptId, UUID studentId, UUID questionId);
}
