package edu.ai.lms.core.coderunner.api;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;


/** Public boundary used by the Attempt module; hidden tests never cross it. */
public interface AttemptCodeRunnerApi {

    CodeRunnerStudentQuestionView getQuestion(UUID attemptId, UUID quizId, UUID questionId);

    CodeRunnerAcceptedView execute(CodeRunnerExecutionRequest request);

    CodeRunnerSubmissionView getSubmission(UUID studentId, UUID submissionId, boolean hidesPrivateGrading);

    boolean isValidSubmit(UUID submissionId, UUID attemptId, UUID studentId, UUID questionId);

    boolean isTerminalSubmit(UUID submissionId, UUID attemptId, UUID studentId, UUID questionId);

    Optional<CodeRunnerSubmissionScoreView> findCompletedSubmit(
            UUID submissionId, UUID attemptId, UUID studentId, UUID questionId);
            
    OptionalDouble findLatestCompletedSubmitRatio(UUID attemptId, UUID questionId);

    boolean hasPendingSubmissions(UUID attemptId);
}
