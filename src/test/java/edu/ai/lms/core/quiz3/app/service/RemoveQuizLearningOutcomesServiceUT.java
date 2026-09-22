package edu.ai.lms.core.quiz3.app.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ai.lms.core.quiz3.app.service.crudquiz.RemoveQuizLearningOutcomesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz3.app.domain.CurrentActor;
import edu.ai.lms.core.quiz3.adapter.in.http.response.QuizLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz3.app.domain.Quiz;
import edu.ai.lms.core.quiz3.app.port.in.command.RemoveQuizLearningOutcomesCommand;
import edu.ai.lms.core.quiz3.app.port.out.LoadCourseLearningOutcomePort;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuizLearningOutcomePort;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz3.app.port.out.SaveQuizLearningOutcomePort;

@ExtendWith(MockitoExtension.class)
class RemoveQuizLearningOutcomesServiceUT {

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadCourseLearningOutcomePort loadCourseLearningOutcomePort;

    @Mock
    private SaveQuizLearningOutcomePort saveQuizLearningOutcomePort;

    @Mock
    private LoadQuizLearningOutcomePort loadQuizLearningOutcomePort;

    @InjectMocks
    private RemoveQuizLearningOutcomesService service;

    private UUID teacherId;
    private UUID courseId;
    private UUID quizId;
    private UUID lo1;
    private UUID lo2;
    private UUID group1;
    private RemoveQuizLearningOutcomesCommand command;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        lo1 = UUID.randomUUID();
        lo2 = UUID.randomUUID();
        group1 = UUID.randomUUID();

        command = RemoveQuizLearningOutcomesCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .learningOutcomeIds(List.of(lo1))
                .groupLearningOutcomeIds(List.of(group1))
                .build();
    }

    @Test
    @DisplayName("Should remove merged and deduplicated learning outcomes")
    void testRemoveLearningOutcomes_Success() {
        CurrentActor teacher = CurrentActor.builder().id(teacherId).role(Roles.TEACHER).build();
        Quiz quiz = Quiz.builder().id(quizId).courseId(courseId).teacherId(teacherId).build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(teacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(quiz));
        when(loadCourseLearningOutcomePort.findExistingGroupLearningOutcomeIdsByCourseIdAndIds(courseId, List.of(group1)))
                .thenReturn(List.of(group1));
        when(loadCourseLearningOutcomePort.findLearningOutcomeIdsByCourseIdAndGroupIds(courseId, List.of(group1)))
                .thenReturn(List.of(lo1, lo2));
        when(loadCourseLearningOutcomePort.findExistingLearningOutcomeIdsByCourseIdAndIds(courseId, List.of(lo1, lo2)))
                .thenReturn(List.of(lo1, lo2));
        when(loadQuizLearningOutcomePort.findLearningOutcomeIdsByQuizId(quizId)).thenReturn(List.of(lo2));

        QuizLearningOutcomeOperationResponse response = service.removeLearningOutcomes(command);

        assertEquals("REMOVE", response.getOperation());
        assertEquals(2, response.getTotalAffected());
        assertEquals(1, response.getLearningOutcomeIds().size());

        verify(saveQuizLearningOutcomePort).removeLearningOutcomes(quizId, List.of(lo1, lo2));
    }

    @Test
    @DisplayName("Should fail when request has no ids")
    void testRemoveLearningOutcomes_EmptyRequest() {
        CurrentActor teacher = CurrentActor.builder().id(teacherId).role(Roles.TEACHER).build();
        Quiz quiz = Quiz.builder().id(quizId).courseId(courseId).teacherId(teacherId).build();

        RemoveQuizLearningOutcomesCommand emptyCommand = RemoveQuizLearningOutcomesCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .learningOutcomeIds(List.of())
                .groupLearningOutcomeIds(List.of())
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(teacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(quiz));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.removeLearningOutcomes(emptyCommand));
        assertEquals("At least one learning outcome or group learning outcome id is required", exception.getMessage());

        verify(saveQuizLearningOutcomePort, never()).removeLearningOutcomes(quizId, List.of());
    }
}
