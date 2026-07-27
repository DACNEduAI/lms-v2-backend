package edu.ai.lms.core.course.app.service.exammatrix;

import java.util.UUID;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixResponse;
import edu.ai.lms.core.course.app.exception.ExamMatrixNotFoundException;
import edu.ai.lms.core.course.app.port.in.usecase.GetExamMatrixUseCase;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetExamMatrixService implements GetExamMatrixUseCase {
    private final LoadExamMatrixPort loadPort;
    private final ExamMatrixAccess access;
    private final ExamMatrixResponseAssembler assembler;

    @Override
    public ExamMatrixResponse getExamMatrix(UUID courseId, UUID matrixId) {
        access.requireCourseTeacher(courseId);
        return assembler.toResponse(loadPort.findByCourseIdAndId(courseId, matrixId)
                .orElseThrow(() -> new ExamMatrixNotFoundException("Exam matrix not found: " + matrixId)));
    }
}
