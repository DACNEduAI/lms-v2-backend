package edu.ai.lms.core.course.app.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.adapter.in.response.ExamMatrixRowResponse;
import edu.ai.lms.core.course.adapter.in.response.ModifyExamMatrixResponse;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.domain.ExamMatrixRow;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.ExamMatrixNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ModifyExamMatrixCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherModifyExamMatrixUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveExamMatrixPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModifyExamMatrixService implements TeacherModifyExamMatrixUseCase {

    private final SaveExamMatrixPort saveExamMatrixPort;
    private final LoadExamMatrixPort loadExamMatrixPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public ModifyExamMatrixResponse modifyExamMatrix(ModifyExamMatrixCommand command) {
        // Validate course exists
        var course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found: " + command.courseId()));

        // Validate authenticated teacher and ownership
        UserAuth user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to manage this course");
        }

        // Validate exam matrix exists and belongs to this course
        ExamMatrix existing = loadExamMatrixPort.findById(command.matrixId())
                .orElseThrow(() -> new ExamMatrixNotFoundException("Exam matrix not found: " + command.matrixId()));

        if (!existing.getCourseId().equals(command.courseId())) {
            throw new IllegalArgumentException("Exam matrix does not belong to this course");
        }

        // Build updated rows (replace-all strategy)
        List<ExamMatrixRow> rows = command.rows().stream()
                .map(r -> ExamMatrixRow.builder()
                        .id(r.id())
                        .matrixId(command.matrixId())
                        .learningOutcomeId(r.learningOutcomeId())
                        .chapterId(r.chapterId())
                        .topic(r.topic())
                        .understandCount(r.understandCount())
                        .applyCount(r.applyCount())
                        .applyPlusCount(r.applyPlusCount())
                        .orderIndex(r.orderIndex())
                        .build())
                .collect(Collectors.toList());

        existing.setTitle(command.title());
        existing.setDescription(command.description());
        existing.setRows(rows);

        ExamMatrix saved = saveExamMatrixPort.save(existing);

        return toResponse(saved);
    }

    private ModifyExamMatrixResponse toResponse(ExamMatrix matrix) {
        List<ExamMatrixRowResponse> rowResponses = matrix.getRows().stream()
                .map(r -> ExamMatrixRowResponse.builder()
                        .id(r.getId())
                        .learningOutcomeId(r.getLearningOutcomeId())
                        .chapterId(r.getChapterId())
                        .topic(r.getTopic())
                        .understandCount(r.getUnderstandCount())
                        .applyCount(r.getApplyCount())
                        .applyPlusCount(r.getApplyPlusCount())
                        .orderIndex(r.getOrderIndex())
                        .build())
                .collect(Collectors.toList());

        return ModifyExamMatrixResponse.builder()
                .id(matrix.getId())
                .courseId(matrix.getCourseId())
                .title(matrix.getTitle())
                .description(matrix.getDescription())
                .rows(rowResponses)
                .updatedAt(matrix.getUpdatedAt())
                .build();
    }
}
