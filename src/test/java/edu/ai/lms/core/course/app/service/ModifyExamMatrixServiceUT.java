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

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.ModifyExamMatrixResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.exception.ExamMatrixNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ExamMatrixRowCommand;
import edu.ai.lms.core.course.app.port.in.command.ModifyExamMatrixCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveExamMatrixPort;

@ExtendWith(MockitoExtension.class)
class ModifyExamMatrixServiceUT {

    @Mock private SaveExamMatrixPort saveExamMatrixPort;
    @Mock private LoadExamMatrixPort loadExamMatrixPort;
    @Mock private LoadCoursePort loadCoursePort;
    @Mock private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private ModifyExamMatrixService modifyExamMatrixService;

    private UUID teacherId;
    private UUID courseId;
    private UUID matrixId;
    private UUID loId;
    private CurrentActor mockTeacher;
    private Course mockCourse;
    private ExamMatrix existingMatrix;
    private ModifyExamMatrixCommand validCommand;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId  = UUID.randomUUID();
        matrixId  = UUID.randomUUID();
        loId      = UUID.randomUUID();

        mockTeacher = CurrentActor.builder().id(teacherId).role(Roles.TEACHER).build();

        mockCourse = Course.builder().id(courseId).teacherIds(List.of(teacherId)).build();

        existingMatrix = ExamMatrix.builder()
                .id(matrixId)
                .courseId(courseId)
                .title("Old Title")
                .rows(List.of())
                .build();

        validCommand = ModifyExamMatrixCommand.builder()
                .matrixId(matrixId)
                .courseId(courseId)
                .title("New Title")
                .description("Updated description")
                .rows(List.of(ExamMatrixRowCommand.builder()
                        .learningOutcomeId(loId)
                        .understandCount(3)
                        .applyCount(2)
                        .applyPlusCount(1)
                        .orderIndex(0)
                        .build()))
                .build();
    }

    @Test
    @DisplayName("Should successfully update an exam matrix")
    void testModify_WithValidDetails_ShouldSucceed() {
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadExamMatrixPort.findById(matrixId)).thenReturn(Optional.of(existingMatrix));

        ExamMatrix updatedMatrix = ExamMatrix.builder()
                .id(matrixId).courseId(courseId).title("New Title").rows(List.of()).build();
        when(saveExamMatrixPort.save(any(ExamMatrix.class))).thenReturn(updatedMatrix);

        ModifyExamMatrixResponse response = modifyExamMatrixService.modifyExamMatrix(validCommand);

        assertNotNull(response);
        assertEquals("New Title", response.getTitle());
        verify(saveExamMatrixPort, times(1)).save(any(ExamMatrix.class));
    }

    @Test
    @DisplayName("Should throw ExamMatrixNotFoundException when matrix not found")
    void testModify_MatrixNotFound_ShouldThrow() {
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadExamMatrixPort.findById(matrixId)).thenReturn(Optional.empty());

        assertThrows(ExamMatrixNotFoundException.class,
                () -> modifyExamMatrixService.modifyExamMatrix(validCommand));

        verify(saveExamMatrixPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when matrix belongs to different course")
    void testModify_MatrixBelongsToDifferentCourse_ShouldThrow() {
        ExamMatrix anotherCourseMatrix = ExamMatrix.builder()
                .id(matrixId).courseId(UUID.randomUUID()).title("X").rows(List.of()).build();

        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadExamMatrixPort.findById(matrixId)).thenReturn(Optional.of(anotherCourseMatrix));

        assertThrows(IllegalArgumentException.class,
                () -> modifyExamMatrixService.modifyExamMatrix(validCommand));

        verify(saveExamMatrixPort, never()).save(any());
    }
}
