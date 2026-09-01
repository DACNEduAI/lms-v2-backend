package edu.ai.lms.core.coderunner.adapter.in.api;

import edu.ai.lms.core.coderunner.api.AttemptCodeRunnerApi;
import edu.ai.lms.core.coderunner.api.CodeRunnerApiException;
import edu.ai.lms.core.coderunner.api.CodeRunnerExecutionRequest;
import edu.ai.lms.core.coderunner.api.CodeRunnerStudentQuestionView;
import edu.ai.lms.core.coderunner.api.CodeRunnerSubmissionScoreView;
import edu.ai.lms.core.coderunner.api.CodeRunnerSubmissionView;
import edu.ai.lms.core.coderunner.api.CodeRunnerAcceptedView;
import edu.ai.lms.core.coderunner.app.domain.CodeSubmission;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerConflictException;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerNotFoundException;
import edu.ai.lms.core.coderunner.app.service.StudentCodeRunnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AttemptCodeRunnerApiAdapter implements AttemptCodeRunnerApi {
    private final StudentCodeRunnerService service;

    @Override public CodeRunnerStudentQuestionView getQuestion(UUID attemptId, UUID quizId, UUID questionId) {
        try {
            return service.getQuestion(attemptId, quizId, questionId);
        } catch (RuntimeException exception) {
            throw apiException(exception);
        }
    }

    @Override public CodeRunnerAcceptedView execute(CodeRunnerExecutionRequest request) {
        try {
            return service.execute(request.attemptId(), request.studentId(), request.quizId(), request.questionId(),
                    request.sourceCode(), request.idempotencyKey(), CodeSubmission.Mode.valueOf(request.mode().name()));
        } catch (RuntimeException exception) {
            throw apiException(exception);
        }
    }

    @Override public CodeRunnerSubmissionView getSubmission(UUID studentId, UUID submissionId, boolean hidesPrivateGrading) {
        try {
            return service.getSubmission(studentId, submissionId, hidesPrivateGrading);
        } catch (RuntimeException exception) {
            throw apiException(exception);
        }
    }

    @Override public boolean isValidSubmit(UUID submissionId, UUID attemptId, UUID studentId, UUID questionId) {
        return service.isValidSubmit(submissionId, attemptId, studentId, questionId);
    }

    @Override public boolean isTerminalSubmit(UUID submissionId, UUID attemptId, UUID studentId, UUID questionId) {
        return service.isTerminalSubmit(submissionId, attemptId, studentId, questionId);
    }

    @Override public Optional<CodeRunnerSubmissionScoreView> findCompletedSubmit(
            UUID submissionId, UUID attemptId, UUID studentId, UUID questionId) {
        return service.findCompletedSubmit(submissionId, attemptId, studentId, questionId);
    }

    @Override public java.util.OptionalDouble findLatestCompletedSubmitRatio(UUID attemptId, UUID questionId) {
        return service.findLatestCompletedSubmitRatio(attemptId, questionId);
    }

    @Override public boolean hasPendingSubmissions(UUID attemptId) {
        return service.hasPendingSubmissions(attemptId);
    }

    private CodeRunnerApiException apiException(RuntimeException exception) {
        CodeRunnerApiException.Kind kind = exception instanceof CodeRunnerNotFoundException
                ? CodeRunnerApiException.Kind.NOT_FOUND
                : exception instanceof CodeRunnerConflictException
                ? CodeRunnerApiException.Kind.CONFLICT
                : exception instanceof IllegalArgumentException
                ? CodeRunnerApiException.Kind.INVALID
                : CodeRunnerApiException.Kind.EXECUTION_FAILED;
        return new CodeRunnerApiException(kind, exception.getMessage(), exception);
    }
}
