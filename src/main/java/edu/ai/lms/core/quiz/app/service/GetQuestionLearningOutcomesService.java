package edu.ai.lms.core.quiz.app.service;

import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuestionLearningOutcomesResponse;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.QuestionLearningOutcomeView;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuestionLearningOutcomesUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetQuestionLearningOutcomesService implements GetQuestionLearningOutcomesUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadQuestionLearningOutcomePort loadQuestionLearningOutcomePort;

    @Override
    @Transactional(readOnly = true)
    public GetQuestionLearningOutcomesResponse getLearningOutcomes(UUID quizId, UUID questionId) {
        CurrentActor user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Question question = loadQuestionPort.getByIdWithoutAnswers(questionId)
                .orElseThrow(() -> new QuestionNotFoundException("Question not found with id: " + questionId));

        if (!question.getQuizId().equals(quizId)) {
            throw new IllegalArgumentException("Question does not belong to this quiz");
        }

        UUID courseId = loadQuizPort.getById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found with id: " + quizId))
                .getCourseId();

        List<GetQuestionLearningOutcomesResponse.LearningOutcomeSummary> learningOutcomes =
                loadQuestionLearningOutcomePort.findLearningOutcomesByQuestionId(questionId)
                        .stream()
                        .map(this::toSummary)
                        .toList();

        return GetQuestionLearningOutcomesResponse.builder()
                .questionId(questionId)
                .quizId(quizId)
                .courseId(courseId)
                .learningOutcomes(learningOutcomes)
                .build();
    }

    private GetQuestionLearningOutcomesResponse.LearningOutcomeSummary toSummary(QuestionLearningOutcomeView view) {
        return GetQuestionLearningOutcomesResponse.LearningOutcomeSummary.builder()
                .id(view.getLearningOutcomeId())
                .code(view.getLearningOutcomeCode())
                .prefixNumber(view.getLearningOutcomePrefixNumber())
                .description(view.getLearningOutcomeDescription())
                .build();
    }
}
