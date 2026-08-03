package edu.ai.lms.core.coderunner.api;

import java.util.Optional;
import java.util.UUID;

/** Public boundary used by the Attempt module; hidden tests never cross it. */
public interface AttemptCodeRunnerApi {

    CodeRunnerStudentQuestionView getQuestion(UUID quizId, UUID questionId);

    CodeRunnerSubmissionView execute(CodeRunnerExecutionRequest request);

    Optional<CodeRunnerSubmissionScoreView> findCompletedSubmit(
            UUID submissionId, UUID attemptId, UUID studentId, UUID questionId);
}
