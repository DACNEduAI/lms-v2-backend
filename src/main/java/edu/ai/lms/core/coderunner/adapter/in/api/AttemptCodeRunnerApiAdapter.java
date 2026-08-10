package edu.ai.lms.core.coderunner.adapter.in.api;

import edu.ai.lms.core.coderunner.api.AttemptCodeRunnerApi;
import edu.ai.lms.core.coderunner.api.CodeRunnerApiException;
import edu.ai.lms.core.coderunner.api.CodeRunnerExecutionRequest;
import edu.ai.lms.core.coderunner.api.CodeRunnerStudentQuestionView;
import edu.ai.lms.core.coderunner.api.CodeRunnerSubmissionScoreView;
import edu.ai.lms.core.coderunner.api.CodeRunnerSubmissionView;
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

    @Override public CodeRunnerStudentQuestionView getQuestion(UUID quizId, UUID questionId) {
        try {
            return service.getQuestion(quizId, questionId);
        } catch (RuntimeException exception) {
            throw apiException(exception);
        }
    }

    @Override public CodeRunnerSubmissionView execute(CodeRunnerExecutionRequest request) {
        try {
            return service.execute(request.attemptId(), request.studentId(), request.quizId(), request.questionId(),
                    request.sourceCode(), request.idempotencyKey(), CodeSubmission.Mode.valueOf(request.mode().name()));
        } catch (RuntimeException exception) {
            throw apiException(exception);
        }
    }

    @Override public Optional<CodeRunnerSubmissionScoreView> findCompletedSubmit(
            UUID submissionId, UUID attemptId, UUID studentId, UUID questionId) {
        return service.findCompletedSubmit(submissionId, attemptId, studentId, questionId);
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
