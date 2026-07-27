package edu.ai.lms.core.course.app.service.exammatrix;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixResponse;
import edu.ai.lms.core.course.app.domain.*;
import edu.ai.lms.core.course.app.port.in.command.CreateExamMatrixCommand;
import edu.ai.lms.core.course.app.port.out.SaveExamMatrixPort;

@ExtendWith(MockitoExtension.class)
class CreateExamMatrixServiceUT {
    @Mock SaveExamMatrixPort savePort;
    @Mock ExamMatrixAccess access;
    @Mock ExamMatrixValidator validator;
    @Mock ExamMatrixResponseAssembler assembler;
    @InjectMocks CreateExamMatrixService service;

    @Test
    void createsValidatedGraphAndReturnsSharedResponse() {
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder().id(courseId).build();
        ExamMatrixRow row = ExamMatrixRow.builder().build();
        ExamMatrix saved = ExamMatrix.builder().id(UUID.randomUUID()).build();
        ExamMatrixResponse expected = ExamMatrixResponse.builder().id(saved.getId()).build();
        var command = CreateExamMatrixCommand.builder().courseId(courseId).title("Matrix")
                .rows(List.of()).build();
        when(access.requireCourseTeacher(courseId)).thenReturn(course);
        when(validator.validateAndMap(course, null, List.of())).thenReturn(List.of(row));
        when(savePort.save(any())).thenReturn(saved);
        when(assembler.toResponse(saved)).thenReturn(expected);

        assertSame(expected, service.createExamMatrix(command));
        verify(savePort).save(argThat(matrix -> matrix.getRows().equals(List.of(row))));
    }
}
