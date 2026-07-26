package edu.ai.lms.core.quiz.adapter.out.integration;

import edu.ai.lms.core.course.api.LearningOutcomeCatalogApi;
import edu.ai.lms.core.course.api.LearningOutcomeView;
import edu.ai.lms.core.quiz.app.domain.QuestionLearningOutcomeLink;
import edu.ai.lms.core.quiz.app.domain.QuestionLearningOutcomeView;
import edu.ai.lms.core.quiz.app.port.out.LoadLearningOutcomeAssociationsPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class QuestionLearningOutcomeCatalogAdapter implements LoadQuestionLearningOutcomePort {

    private final LoadLearningOutcomeAssociationsPort associations;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuizPort loadQuizPort;
    private final LearningOutcomeCatalogApi catalogApi;

    @Override
    public List<QuestionLearningOutcomeView> findLearningOutcomesByQuestionId(UUID questionId) {
        var question = loadQuestionPort.getByIdWithoutAnswers(questionId).orElse(null);
        if (question == null) {
            return List.of();
        }
        var quiz = loadQuizPort.getById(question.getQuizId()).orElse(null);
        if (quiz == null) {
            return List.of();
        }
        return catalogApi.findByIds(quiz.getCourseId(), associations.findByQuestionId(questionId)).stream()
                .map(outcome -> toView(questionId, outcome))
                .toList();
    }

    @Override
    public List<UUID> findLearningOutcomeIdsByQuestionId(UUID questionId) {
        return associations.findByQuestionId(questionId);
    }

    @Override
    public List<QuestionLearningOutcomeView> findLearningOutcomesByQuizId(UUID quizId) {
        var quiz = loadQuizPort.getById(quizId).orElse(null);
        if (quiz == null) {
            return List.of();
        }
        List<QuestionLearningOutcomeLink> links = associations.findByQuizId(quizId);
        Map<UUID, LearningOutcomeView> outcomes = catalogApi.findByIds(
                        quiz.getCourseId(),
                        links.stream().map(QuestionLearningOutcomeLink::learningOutcomeId).toList())
                .stream()
                .collect(Collectors.toMap(LearningOutcomeView::id, Function.identity()));
        return links.stream()
                .filter(link -> outcomes.containsKey(link.learningOutcomeId()))
                .map(link -> toView(link.questionId(), outcomes.get(link.learningOutcomeId())))
                .toList();
    }

    private QuestionLearningOutcomeView toView(UUID questionId, LearningOutcomeView outcome) {
        return QuestionLearningOutcomeView.builder()
                .questionId(questionId)
                .learningOutcomeId(outcome.id())
                .learningOutcomeCode(outcome.code())
                .learningOutcomePrefixNumber(outcome.prefixNumber())
                .learningOutcomeDescription(outcome.description())
                .build();
    }
}
