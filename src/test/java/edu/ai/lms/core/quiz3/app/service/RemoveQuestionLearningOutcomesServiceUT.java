package edu.ai.lms.core.quiz3.app.service;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz3.app.domain.CurrentActor;
import edu.ai.lms.core.quiz3.adapter.in.http.response.QuestionLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz3.app.domain.Question;
import edu.ai.lms.core.quiz3.app.domain.Quiz;
import edu.ai.lms.core.quiz3.app.port.in.command.RemoveQuestionLearningOutcomesCommand;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuestionLearningOutcomePort;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz3.app.port.out.SaveQuestionLearningOutcomePort;
import edu.ai.lms.core.quiz3.app.service.crudquiz.RemoveQuestionLearningOutcomesService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveQuestionLearningOutcomesServiceUT {

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;
    @Mock
    private LoadQuizPort loadQuizPort;
    @Mock
    private LoadQuestionPort loadQuestionPort;
    @Mock
    private SaveQuestionLearningOutcomePort saveQuestionLearningOutcomePort;
    @Mock
    private LoadQuestionLearningOutcomePort loadQuestionLearningOutcomePort;

    @InjectMocks
    private RemoveQuestionLearningOutcomesService service;

    @Test
    @DisplayName("Should remove learning outcomes from question successfully")
    void shouldRemoveLearningOutcomesSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID lo1 = UUID.randomUUID();
        UUID lo2 = UUID.randomUUID();

        when(loadSignedInUserPort.getAuthenticatedUser())
                .thenReturn(Optional.of(CurrentActor.builder().id(userId).role(Roles.TEACHER).build()));
        when(loadQuizPort.getById(quizId))
                .thenReturn(Optional.of(Quiz.builder().id(quizId).courseId(courseId).teacherId(userId).build()));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId))
                .thenReturn(Optional.of(Question.builder().id(questionId).quizId(quizId).build()));
        when(loadQuestionLearningOutcomePort.findLearningOutcomeIdsByQuestionId(questionId))
                .thenReturn(List.of(lo2));

        QuestionLearningOutcomeOperationResponse response = service.removeLearningOutcomes(
                RemoveQuestionLearningOutcomesCommand.builder()
                        .quizId(quizId)
                        .questionId(questionId)
                        .learningOutcomeIds(List.of(lo1))
                        .build());

        assertEquals("REMOVE", response.getOperation());
        assertEquals(1, response.getTotalAffected());
        assertEquals(List.of(lo2), response.getLearningOutcomeIds());
        verify(saveQuestionLearningOutcomePort).removeLearningOutcomes(questionId, List.of(lo1));
    }
}
