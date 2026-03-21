package edu.ai.lms.core.quiz.app.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.port.in.command.AddQuizLearningOutcomesCommand;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizLearningOutcomePort;

@ExtendWith(MockitoExtension.class)
class AddQuizLearningOutcomesServiceUT {

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
    private AddQuizLearningOutcomesService service;

    private UUID teacherId;
    private UUID courseId;
    private UUID quizId;
    private UUID lo1;
    private UUID lo2;
    private UUID lo3;
    private UUID group1;
    private AddQuizLearningOutcomesCommand command;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        lo1 = UUID.randomUUID();
        lo2 = UUID.randomUUID();
        lo3 = UUID.randomUUID();
        group1 = UUID.randomUUID();

        command = AddQuizLearningOutcomesCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .learningOutcomeIds(List.of(lo1, lo2))
                .groupLearningOutcomeIds(List.of(group1))
                .build();
    }

    @Test
    @DisplayName("Should add merged and deduplicated learning outcomes")
    void testAddLearningOutcomes_Success() {
        UserAuth teacher = UserAuth.builder().id(teacherId).role(Roles.TEACHER).build();
        Quiz quiz = Quiz.builder().id(quizId).courseId(courseId).teacherId(teacherId).build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(teacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(quiz));
        when(loadCourseLearningOutcomePort.findExistingGroupLearningOutcomeIdsByCourseIdAndIds(courseId, List.of(group1)))
                .thenReturn(List.of(group1));
        when(loadCourseLearningOutcomePort.findLearningOutcomeIdsByCourseIdAndGroupIds(courseId, List.of(group1)))
                .thenReturn(List.of(lo2, lo3));
        when(loadCourseLearningOutcomePort.findExistingLearningOutcomeIdsByCourseIdAndIds(courseId, List.of(lo1, lo2, lo3)))
                .thenReturn(List.of(lo1, lo2, lo3));
        when(loadQuizLearningOutcomePort.findLearningOutcomeIdsByQuizId(quizId)).thenReturn(List.of(lo1, lo2, lo3));

        QuizLearningOutcomeOperationResponse response = service.addLearningOutcomes(command);

        assertEquals("ADD", response.getOperation());
        assertEquals(3, response.getTotalAffected());
        assertEquals(3, response.getLearningOutcomeIds().size());

        verify(saveQuizLearningOutcomePort).addLearningOutcomes(quizId, List.of(lo1, lo2, lo3));
    }

    @Test
    @DisplayName("Should fail when user is not owner")
    void testAddLearningOutcomes_NotOwner() {
        UUID anotherTeacherId = UUID.randomUUID();
        UserAuth anotherTeacher = UserAuth.builder().id(anotherTeacherId).role(Roles.TEACHER).build();
        Quiz quiz = Quiz.builder().id(quizId).courseId(courseId).teacherId(teacherId).build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(anotherTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(quiz));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.addLearningOutcomes(command));
        assertEquals("User is not authorized to modify this quiz", exception.getMessage());

        verify(saveQuizLearningOutcomePort, never()).addLearningOutcomes(quizId, List.of(lo1, lo2, lo3));
    }

    @Test
    @DisplayName("Should fail when any learning outcome belongs to another course")
    void testAddLearningOutcomes_LearningOutcomeFromAnotherCourse() {
        UserAuth teacher = UserAuth.builder().id(teacherId).role(Roles.TEACHER).build();
        Quiz quiz = Quiz.builder().id(quizId).courseId(courseId).teacherId(teacherId).build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(teacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(quiz));
        when(loadCourseLearningOutcomePort.findExistingGroupLearningOutcomeIdsByCourseIdAndIds(courseId, List.of(group1)))
                .thenReturn(List.of(group1));
        when(loadCourseLearningOutcomePort.findLearningOutcomeIdsByCourseIdAndGroupIds(courseId, List.of(group1)))
                .thenReturn(List.of(lo2, lo3));
        when(loadCourseLearningOutcomePort.findExistingLearningOutcomeIdsByCourseIdAndIds(courseId, List.of(lo1, lo2, lo3)))
                .thenReturn(List.of(lo1, lo2));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.addLearningOutcomes(command));

        assertEquals("Some learning outcomes do not belong to this course", exception.getMessage());
        verify(saveQuizLearningOutcomePort, never()).addLearningOutcomes(quizId, List.of(lo1, lo2, lo3));
    }
}
