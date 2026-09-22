package edu.ai.lms.core.coderunner.adapter.out.integration;

import edu.ai.lms.core.coderunner.app.domain.CodeRunnerQuestionContext;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerQuestionContextPort;
import edu.ai.lms.core.quiz3.api.CodeRunnerQuestionApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class CodeRunnerQuestionContextAdapter implements LoadCodeRunnerQuestionContextPort {

    private final CodeRunnerQuestionApi questionApi;

    @Override
    public Optional<CodeRunnerQuestionContext> findQuestionContext(UUID questionId) {
        return questionApi.findContext(questionId).map(context -> new CodeRunnerQuestionContext(
                context.questionId(),
                context.quizId(),
                context.courseId(),
                context.teacherId(),
                context.questionType(),
                context.questionReviewStatus(),
                context.quizLifecycleStatus(),
                context.questionVersion(),
                context.draftContractJson()));
    }
}
