package edu.ai.lms.core.quiz3.adapter.in.api;

import edu.ai.lms.core.quiz3.api.CodeRunnerQuestionApi;
import edu.ai.lms.core.quiz3.api.CodeRunnerQuestionContext;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuizPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class CodeRunnerQuestionApiAdapter implements CodeRunnerQuestionApi {

    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuizPort loadQuizPort;

    @Override
    public Optional<CodeRunnerQuestionContext> findContext(UUID questionId) {
        return loadQuestionPort.getByIdWithoutAnswers(questionId).flatMap(question ->
                loadQuizPort.getById(question.getQuizId()).map(quiz -> new CodeRunnerQuestionContext(
                        question.getId(),
                        quiz.getId(),
                        quiz.getCourseId(),
                        quiz.getTeacherId(),
                        question.getType() == null ? null : question.getType().name(),
                        question.getReviewStatus() == null ? null : question.getReviewStatus().name(),
                        quiz.getLifecycleStatus() == null ? null : quiz.getLifecycleStatus().name(),
                        question.getVersion(),
                        null)));
    }
}
