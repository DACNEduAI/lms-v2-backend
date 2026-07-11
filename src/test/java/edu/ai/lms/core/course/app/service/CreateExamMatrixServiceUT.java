package edu.ai.lms.core.course.app.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.adapter.in.response.CreateExamMatrixResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.exception.UnauthorizedTeacherException;
import edu.ai.lms.core.course.app.port.in.command.CreateExamMatrixCommand;
import edu.ai.lms.core.course.app.port.in.command.ExamMatrixRowCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveExamMatrixPort;

@ExtendWith(MockitoExtension.class)
class CreateExamMatrixServiceUT {

    @Mock private SaveExamMatrixPort saveExamMatrixPort;
    @Mock private LoadCoursePort loadCoursePort;
    @Mock private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private CreateExamMatrixService createExamMatrixService;

    private UUID teacherId;
    private UUID courseId;
    private UUID matrixId;
    private UUID loId;
    private UserAuth mockTeacher;
    private Course mockCourse;
    private CreateExamMatrixCommand validCommand;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId  = UUID.randomUUID();
        matrixId  = UUID.randomUUID();
        loId      = UUID.randomUUID();

        mockTeacher = UserAuth.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        mockCourse = Course.builder()
                .id(courseId)
                .teacherIds(List.of(teacherId))
                .learningOutcomes(List.of(LearningOutcome.builder().id(loId).build()))
                .build();

        validCommand = CreateExamMatrixCommand.builder()
                .courseId(courseId)
                .title("Midterm Matrix")
                .description("Mid-semester exam matrix")
                .rows(List.of(ExamMatrixRowCommand.builder()
                        .learningOutcomeId(loId)
                        .understandCount(2)
                        .applyCount(1)
                        .applyPlusCount(0)
                        .orderIndex(0)
                        .build()))
                .build();
    }

    @Test
    @DisplayName("Should successfully create an exam matrix")
    void testCreate_WithValidDetails_ShouldSucceed() {
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));

        ExamMatrix savedMatrix = ExamMatrix.builder()
                .id(matrixId)
                .courseId(courseId)
                .title("Midterm Matrix")
                .description("Mid-semester exam matrix")
                .rows(List.of())
                .build();
        when(saveExamMatrixPort.save(any(ExamMatrix.class))).thenReturn(savedMatrix);

        CreateExamMatrixResponse response = createExamMatrixService.createExamMatrix(validCommand);

        assertNotNull(response);
        assertEquals(matrixId, response.getId());
        assertEquals("Midterm Matrix", response.getTitle());
        verify(saveExamMatrixPort, times(1)).save(any(ExamMatrix.class));
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testCreate_CourseNotFound_ShouldThrow() {
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class,
                () -> createExamMatrixService.createExamMatrix(validCommand));

        verify(saveExamMatrixPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when not authenticated")
    void testCreate_TeacherNotAuthenticated_ShouldThrow() {
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        assertThrows(TeacherNotFoundException.class,
                () -> createExamMatrixService.createExamMatrix(validCommand));

        verify(saveExamMatrixPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedTeacherException when teacher does not own course")
    void testCreate_TeacherNotOwner_ShouldThrow() {
        Course otherCourse = Course.builder()
                .id(courseId)
                .teacherIds(List.of(UUID.randomUUID())) // different teacher
                .build();

        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(otherCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));

        assertThrows(UnauthorizedTeacherException.class,
                () -> createExamMatrixService.createExamMatrix(validCommand));

        verify(saveExamMatrixPort, never()).save(any());
    }
}
