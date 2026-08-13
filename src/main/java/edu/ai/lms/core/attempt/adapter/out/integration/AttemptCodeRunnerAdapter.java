package edu.ai.lms.core.attempt.adapter.out.integration;

import edu.ai.lms.core.attempt.app.domain.AttemptCodeRunnerQuestion;
import edu.ai.lms.core.attempt.app.domain.AttemptCodeSubmission;
import edu.ai.lms.core.attempt.app.port.out.AttemptCodeRunnerPort;
import edu.ai.lms.core.coderunner.api.AttemptCodeRunnerApi;
import edu.ai.lms.core.coderunner.api.CodeRunnerExecutionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.OptionalDouble;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AttemptCodeRunnerAdapter implements AttemptCodeRunnerPort {
    private final AttemptCodeRunnerApi api;

    @Override public AttemptCodeRunnerQuestion getQuestion(UUID attemptId, UUID quizId, UUID questionId) {
        var view = api.getQuestion(attemptId, quizId, questionId);
        return new AttemptCodeRunnerQuestion(view.questionId(), view.title(), view.statement(), view.starterCode(),
                view.entryPoint(), view.executionProfile(), view.publicTests().stream()
                .map(test -> new AttemptCodeRunnerQuestion.PublicTest(test.id(), test.category(),
                        test.argumentsJson(), test.expectedOutputJson())).toList());
    }

    @Override public AttemptCodeSubmission execute(UUID attemptId, UUID studentId, UUID quizId, UUID questionId,
                                                    String source, String key, String mode) {
        var view = api.execute(new CodeRunnerExecutionRequest(attemptId, studentId, quizId, questionId,
                source, key, CodeRunnerExecutionRequest.Mode.valueOf(mode)));
        return new AttemptCodeSubmission(view.submissionId(), view.questionId(), view.mode(), view.status(),
                view.verdict(), view.score(), view.maxScore(), view.passedTests(), view.totalTests(),
                view.visibleResults().stream().map(result -> new AttemptCodeSubmission.VisibleTestResult(
                        result.testId(), result.verdict(), result.timeMs(), result.memoryKb(), result.diagnostic()))
                        .toList());
    }

    @Override public OptionalDouble findCompletedSubmitRatio(UUID submissionId, UUID attemptId,
                                                              UUID studentId, UUID questionId) {
        return api.findCompletedSubmit(submissionId, attemptId, studentId, questionId)
                .map(score -> OptionalDouble.of(score.ratio())).orElseGet(OptionalDouble::empty);
    }
}
