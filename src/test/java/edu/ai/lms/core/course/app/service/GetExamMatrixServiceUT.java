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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.exception.ExamMatrixNotFoundException;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;

@ExtendWith(MockitoExtension.class)
class GetExamMatrixServiceUT {

    @Mock private LoadExamMatrixPort loadExamMatrixPort;
    @Mock private LoadCoursePort loadCoursePort;

    @InjectMocks
    private GetExamMatrixService getExamMatrixService;

    private UUID courseId;
    private UUID matrixId;
    private Course mockCourse;
    private ExamMatrix mockMatrix;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        matrixId = UUID.randomUUID();

        mockCourse = Course.builder().id(courseId).teacherIds(List.of()).build();
        mockMatrix = ExamMatrix.builder()
                .id(matrixId)
                .courseId(courseId)
                .title("Matrix 1")
                .rows(List.of())
                .build();
    }

    @Test
    @DisplayName("Should return the exam matrix when found")
    void testGet_WithValidIds_ShouldReturnMatrix() {
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadExamMatrixPort.findById(matrixId)).thenReturn(Optional.of(mockMatrix));

        ExamMatrixResponse response = getExamMatrixService.getExamMatrix(courseId, matrixId);

        assertNotNull(response);
        assertEquals(matrixId, response.getId());
        assertEquals("Matrix 1", response.getTitle());
    }

    @Test
    @DisplayName("Should throw ExamMatrixNotFoundException when matrix does not belong to course")
    void testGet_MatrixNotInCourse_ShouldThrow() {
        ExamMatrix wrongCourseMatrix = ExamMatrix.builder()
                .id(matrixId).courseId(UUID.randomUUID()).rows(List.of()).build();

        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadExamMatrixPort.findById(matrixId)).thenReturn(Optional.of(wrongCourseMatrix));

        assertThrows(ExamMatrixNotFoundException.class,
                () -> getExamMatrixService.getExamMatrix(courseId, matrixId));
    }
}
