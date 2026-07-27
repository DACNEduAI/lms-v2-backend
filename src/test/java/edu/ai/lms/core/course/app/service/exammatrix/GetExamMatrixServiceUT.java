package edu.ai.lms.core.course.app.service.exammatrix;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixResponse;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.exception.ExamMatrixNotFoundException;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;

@ExtendWith(MockitoExtension.class)
class GetExamMatrixServiceUT {
    @Mock LoadExamMatrixPort loadPort;
    @Mock ExamMatrixAccess access;
    @Mock ExamMatrixResponseAssembler assembler;
    @InjectMocks GetExamMatrixService service;

    @Test
    void scopesMatrixToCourse() {
        UUID courseId = UUID.randomUUID(), matrixId = UUID.randomUUID();
        when(loadPort.findByCourseIdAndId(courseId, matrixId)).thenReturn(Optional.empty());
        assertThrows(ExamMatrixNotFoundException.class, () -> service.getExamMatrix(courseId, matrixId));
        verify(access).requireCourseTeacher(courseId);
    }

    @Test
    void returnsAssembledMatrix() {
        UUID courseId = UUID.randomUUID(), matrixId = UUID.randomUUID();
        ExamMatrix matrix = ExamMatrix.builder().id(matrixId).build();
        ExamMatrixResponse response = ExamMatrixResponse.builder().id(matrixId).build();
        when(loadPort.findByCourseIdAndId(courseId, matrixId)).thenReturn(Optional.of(matrix));
        when(assembler.toResponse(matrix)).thenReturn(response);
        assertSame(response, service.getExamMatrix(courseId, matrixId));
    }
}
