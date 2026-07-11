package edu.ai.lms.core.course.app.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.adapter.in.response.CreateExamMatrixResponse;
import edu.ai.lms.core.course.adapter.in.response.ExamMatrixRowResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.domain.ExamMatrixRow;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.ChapterNotFoundException;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.exception.UnauthorizedTeacherException;
import edu.ai.lms.core.course.app.port.in.command.CreateExamMatrixCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateExamMatrixUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveExamMatrixPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateExamMatrixService implements TeacherCreateExamMatrixUseCase {

    private final SaveExamMatrixPort saveExamMatrixPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public CreateExamMatrixResponse createExamMatrix(CreateExamMatrixCommand command) {
        // Validate course exists
        var course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException(
                        "Course not found: " + command.courseId()));

        // Validate authenticated teacher and ownership
        UserAuth user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new UnauthorizedTeacherException("Teacher is not authorized to manage this course");
        }

        // Validate chapterId and learningOutcomeId belong to the course
        Set<UUID> courseChapterIds = course.getChapters().stream()
                .map(Chapter::getChapterId)
                .collect(Collectors.toSet());
        Set<UUID> courseLearningOutcomeIds = course.getLearningOutcomes().stream()
                .map(LearningOutcome::getId)
                .collect(Collectors.toSet());

        for (var row : command.rows()) {
            if (row.chapterId() != null && !courseChapterIds.contains(row.chapterId())) {
                throw new ChapterNotFoundException("Chapter not found in the course: " + row.chapterId());
            }
            if (row.learningOutcomeId() != null && !courseLearningOutcomeIds.contains(row.learningOutcomeId())) {
                throw new LearningOutcomeNotFoundException("Learning outcome not found in the course: " + row.learningOutcomeId());
            }
        }

        // Build domain object
        List<ExamMatrixRow> rows = command.rows().stream()
                .map(r -> ExamMatrixRow.builder()
                        .learningOutcomeId(r.learningOutcomeId())
                        .chapterId(r.chapterId())
                        .topic(r.topic())
                        .understandCount(r.understandCount())
                        .applyCount(r.applyCount())
                        .applyPlusCount(r.applyPlusCount())
                        .orderIndex(r.orderIndex())
                        .build())
                .collect(Collectors.toList());

        ExamMatrix examMatrix = ExamMatrix.builder()
                .courseId(command.courseId())
                .title(command.title())
                .description(command.description())
                .rows(rows)
                .build();

        ExamMatrix saved = saveExamMatrixPort.save(examMatrix);

        return toResponse(saved);
    }

    private CreateExamMatrixResponse toResponse(ExamMatrix matrix) {
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

        return CreateExamMatrixResponse.builder()
                .id(matrix.getId())
                .courseId(matrix.getCourseId())
                .title(matrix.getTitle())
                .description(matrix.getDescription())
                .rows(rowResponses)
                .createdAt(matrix.getCreatedAt())
                .build();
    }
}
