package edu.ai.lms.core.quiz.app.service;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuestionLearningOutcomesResponse;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.QuestionLearningOutcomeView;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetQuestionLearningOutcomesServiceUT {

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;
    @Mock
    private LoadQuestionPort loadQuestionPort;
    @Mock
    private LoadQuizPort loadQuizPort;
    @Mock
    private LoadQuestionLearningOutcomePort loadQuestionLearningOutcomePort;

    @InjectMocks
    private GetQuestionLearningOutcomesService service;

    @Test
    @DisplayName("Should get all learning outcomes of question")
    void shouldGetLearningOutcomesOfQuestion() {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID loId = UUID.randomUUID();

        when(loadSignedInUserPort.getAuthenticatedUser())
                .thenReturn(Optional.of(UserAuth.builder().id(userId).role(Roles.TEACHER).build()));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId))
                .thenReturn(Optional.of(Question.builder().id(questionId).quizId(quizId).build()));
        when(loadQuizPort.getById(quizId))
                .thenReturn(Optional.of(Quiz.builder().id(quizId).courseId(courseId).build()));
        when(loadQuestionLearningOutcomePort.findLearningOutcomesByQuestionId(questionId))
                .thenReturn(List.of(QuestionLearningOutcomeView.builder()
                        .learningOutcomeId(loId)
                        .learningOutcomeCode("CLO-1")
                        .learningOutcomeDescription("Understand basics")
                        .build()));

        GetQuestionLearningOutcomesResponse response = service.getLearningOutcomes(quizId, questionId);

        assertNotNull(response);
        assertEquals(questionId, response.getQuestionId());
        assertEquals(quizId, response.getQuizId());
        assertEquals(courseId, response.getCourseId());
        assertEquals(1, response.getLearningOutcomes().size());
        assertEquals(loId, response.getLearningOutcomes().get(0).getId());
        assertEquals("CLO-1", response.getLearningOutcomes().get(0).getCode());
    }
}
