package edu.ai.lms.core.course.app.service;

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
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixListResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;

@ExtendWith(MockitoExtension.class)
class GetAllExamMatricesServiceUT {

    @Mock private LoadExamMatrixPort loadExamMatrixPort;
    @Mock private LoadCoursePort loadCoursePort;
    @Mock private GetExamMatrixService getExamMatrixService;

    @InjectMocks
    private GetAllExamMatricesService getAllExamMatricesService;

    private UUID courseId;
    private Course mockCourse;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        mockCourse = Course.builder().id(courseId).teacherIds(List.of()).build();
    }

    @Test
    @DisplayName("Should return all exam matrices for a course")
    void testGetAll_WithExistingCourse_ShouldReturnList() {
        ExamMatrix matrix1 = ExamMatrix.builder().id(UUID.randomUUID()).courseId(courseId).title("M1").rows(List.of()).build();
        ExamMatrix matrix2 = ExamMatrix.builder().id(UUID.randomUUID()).courseId(courseId).title("M2").rows(List.of()).build();

        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadExamMatrixPort.findAllByCourseId(courseId)).thenReturn(List.of(matrix1, matrix2));

        ExamMatrixListResponse response = getAllExamMatricesService.getAllExamMatrices(courseId);

        assertEquals(courseId, response.getCourseId());
        assertEquals(2, response.getTotal());
    }

    @Test
    @DisplayName("Should return empty list when no matrices exist")
    void testGetAll_NoMatrices_ShouldReturnEmptyList() {
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadExamMatrixPort.findAllByCourseId(courseId)).thenReturn(List.of());

        ExamMatrixListResponse response = getAllExamMatricesService.getAllExamMatrices(courseId);

        assertEquals(0, response.getTotal());
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testGetAll_CourseNotFound_ShouldThrow() {
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class,
                () -> getAllExamMatricesService.getAllExamMatrices(courseId));
    }
}
