package edu.ai.lms.core.course.app.service.exammatrix;

import java.util.UUID;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixListResponse;
import edu.ai.lms.core.course.app.port.in.usecase.GetAllExamMatricesUseCase;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAllExamMatricesService implements GetAllExamMatricesUseCase {
    private final LoadExamMatrixPort loadPort;
    private final ExamMatrixAccess access;
    private final ExamMatrixResponseAssembler assembler;

    @Override
    public ExamMatrixListResponse getAllExamMatrices(UUID courseId) {
        access.requireCourseTeacher(courseId);
        var summaries = loadPort.findAllSummariesByCourseId(courseId);
        return ExamMatrixListResponse.builder().courseId(courseId).total(summaries.size())
                .matrices(summaries.stream().map(assembler::toSummary).toList()).build();
    }
}
