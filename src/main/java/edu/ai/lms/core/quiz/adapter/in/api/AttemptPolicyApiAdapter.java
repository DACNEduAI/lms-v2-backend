package edu.ai.lms.core.quiz.adapter.in.api;

import edu.ai.lms.core.quiz.api.AttemptPolicyApi;
import edu.ai.lms.core.quiz.api.AttemptPolicyView;
import edu.ai.lms.core.quiz.api.QuestionAnswerKeyView;
import edu.ai.lms.core.quiz.api.LearningOutcomeModelView;
import edu.ai.lms.core.course.api.LearningOutcomeCatalogApi;
import edu.ai.lms.core.quiz.app.port.out.LoadAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.ManageQuizReleasePort;
import edu.ai.lms.core.quiz.app.port.out.LoadLearningOutcomeAssociationsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AttemptPolicyApiAdapter implements AttemptPolicyApi {

    private final LoadQuizPort loadQuizPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadAnswerPort loadAnswerPort;
    private final ManageQuizReleasePort releasePort;
    private final LoadLearningOutcomeAssociationsPort learningOutcomeAssociationsPort;
    private final LearningOutcomeCatalogApi learningOutcomeCatalogApi;

    @Override
    public Optional<AttemptPolicyView> findPolicy(UUID quizId) {
        return loadQuizPort.getById(quizId)
                .map(quiz -> new AttemptPolicyView(
                        quiz.getId(),
                        releasePort.findActiveReleaseId(quiz.getId()).orElse(null),
                        quiz.getCourseId(),
                        Boolean.TRUE.equals(quiz.getUnlimited()),
                        quiz.getTimeLimitMinutes(),
                        quiz.getStartTime(),
                        quiz.getDueTime(),
                        quiz.getAttemptLimited()));
    }

    @Override
    public List<QuestionAnswerKeyView> findAnswerKeys(UUID quizId) {
        var quiz = loadQuizPort.getById(quizId).orElseThrow();
        List<QuestionAnswerKeyView> keys = new ArrayList<>();
        loadQuestionPort.getByQuizId(quizId).forEach(question -> {
            var outcomeIds = learningOutcomeAssociationsPort.findByQuestionId(question.getId());
            var outcomes = learningOutcomeCatalogApi.findByIds(quiz.getCourseId(), outcomeIds).stream()
                    .map(outcome -> new LearningOutcomeModelView(
                            outcome.id(), outcome.ktLearningOutcomeId()))
                    .toList();
            var correctAnswers = loadAnswerPort.getByQuestionId(question.getId()).stream()
                    .filter(answer -> answer.isCorrect())
                    .toList();
            if (correctAnswers.isEmpty()) {
                keys.add(new QuestionAnswerKeyView(
                        question.getId(), question.getKtQuestionId(), question.getType().name(), null, outcomes));
            } else {
                correctAnswers.forEach(answer -> keys.add(new QuestionAnswerKeyView(
                        question.getId(), question.getKtQuestionId(), question.getType().name(),
                        answer.getAnswerNumber(), outcomes)));
            }
        });
        return List.copyOf(keys);
    }
}
