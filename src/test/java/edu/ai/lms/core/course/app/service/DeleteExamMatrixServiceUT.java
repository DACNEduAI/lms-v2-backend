package edu.ai.lms.core.course.app.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.DeleteExamMatrixResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.exception.ExamMatrixNotFoundException;
import edu.ai.lms.core.course.app.port.out.DeleteExamMatrixPort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;

@ExtendWith(MockitoExtension.class)
class DeleteExamMatrixServiceUT {

    @Mock private DeleteExamMatrixPort deleteExamMatrixPort;
    @Mock private LoadExamMatrixPort loadExamMatrixPort;
    @Mock private LoadCoursePort loadCoursePort;
    @Mock private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private DeleteExamMatrixService deleteExamMatrixService;

    private UUID teacherId;
    private UUID courseId;
    private UUID matrixId;
    private CurrentActor mockTeacher;
    private Course mockCourse;
    private ExamMatrix existingMatrix;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId  = UUID.randomUUID();
        matrixId  = UUID.randomUUID();

        mockTeacher = CurrentActor.builder().id(teacherId).role(Roles.TEACHER).build();
        mockCourse  = Course.builder().id(courseId).teacherIds(List.of(teacherId)).build();
        existingMatrix = ExamMatrix.builder().id(matrixId).courseId(courseId).rows(List.of()).build();
    }

    @Test
    @DisplayName("Should successfully delete an exam matrix")
    void testDelete_WithValidDetails_ShouldSucceed() {
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadExamMatrixPort.findById(matrixId)).thenReturn(Optional.of(existingMatrix));

        DeleteExamMatrixResponse response = deleteExamMatrixService.deleteExamMatrix(courseId, matrixId);

        assertTrue(response.isSuccess());
        assertEquals(matrixId, response.getMatrixId());
        verify(deleteExamMatrixPort, times(1)).deleteById(matrixId);
    }

    @Test
    @DisplayName("Should throw ExamMatrixNotFoundException when matrix not found")
    void testDelete_MatrixNotFound_ShouldThrow() {
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadExamMatrixPort.findById(matrixId)).thenReturn(Optional.empty());

        assertThrows(ExamMatrixNotFoundException.class,
                () -> deleteExamMatrixService.deleteExamMatrix(courseId, matrixId));

        verify(deleteExamMatrixPort, never()).deleteById(matrixId);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher does not own course")
    void testDelete_TeacherNotOwner_ShouldThrow() {
        Course otherCourse = Course.builder().id(courseId).teacherIds(List.of(UUID.randomUUID())).build();
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(otherCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));

        assertThrows(IllegalArgumentException.class,
                () -> deleteExamMatrixService.deleteExamMatrix(courseId, matrixId));

        verify(deleteExamMatrixPort, never()).deleteById(matrixId);
    }
}
