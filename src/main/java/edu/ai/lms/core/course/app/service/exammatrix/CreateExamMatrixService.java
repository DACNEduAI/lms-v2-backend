package edu.ai.lms.core.course.app.service.exammatrix;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixResponse;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.port.in.command.CreateExamMatrixCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateExamMatrixUseCase;
import edu.ai.lms.core.course.app.port.out.SaveExamMatrixPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateExamMatrixService implements TeacherCreateExamMatrixUseCase {
    private final SaveExamMatrixPort savePort;
    private final ExamMatrixAccess access;
    private final ExamMatrixValidator validator;
    private final ExamMatrixResponseAssembler assembler;

    @Override
    @Transactional
    public ExamMatrixResponse createExamMatrix(CreateExamMatrixCommand command) {
        var course = access.requireCourseTeacher(command.courseId());
        var rows = validator.validateAndMap(course, null, command.rows());
        ExamMatrix matrix = ExamMatrix.builder()
                .courseId(command.courseId()).title(command.title())
                .description(command.description()).rows(rows).build();
        return assembler.toResponse(savePort.save(matrix));
    }
}
