package edu.ai.lms.core.quiz.adapter.out.integration;

import edu.ai.lms.core.course.api.CourseAccessApi;
import edu.ai.lms.core.course.api.LearningOutcomeCatalogApi;
import edu.ai.lms.core.course.api.QuizGenerationContextApi;
import edu.ai.lms.core.material.api.MaterialSelectionApi;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationContextPort;
import edu.ai.lms.core.quiz.app.port.out.QuizGenerationContextView;
import edu.ai.lms.core.quiz.app.port.out.ValidateLearningOutcomesPort;
import edu.ai.lms.core.quiz.app.port.out.ValidateMaterialSelectionPort;
import edu.ai.lms.core.quiz.app.exception.InvalidQuizGenerationRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class QuizReferenceDataAdapter implements LoadCourseMembershipPort, LoadQuizGenerationContextPort,
        ValidateMaterialSelectionPort, ValidateLearningOutcomesPort {

    private final CourseAccessApi courseAccessApi;
    private final QuizGenerationContextApi generationContextApi;
    private final LearningOutcomeCatalogApi learningOutcomeCatalogApi;
    private final MaterialSelectionApi materialSelectionApi;

    @Override
    public boolean isTeacherInCourse(UUID teacherId, UUID courseId) {
        return courseAccessApi.isTeacherInCourse(teacherId, courseId);
    }

    @Override
    public boolean isStudentInCourse(UUID studentId, UUID courseId) {
        return courseAccessApi.isStudentInCourse(studentId, courseId);
    }

    @Override
    public List<UUID> getStudentIdsByCourseId(UUID courseId) {
        return courseAccessApi.getStudentIds(courseId);
    }

    @Override
    public Optional<QuizGenerationContextView> load(UUID courseId, UUID examMatrixId) {
        try {
            return generationContextApi.load(courseId, examMatrixId)
                    .map(context -> new QuizGenerationContextView(
                        context.courseId(),
                        context.courseTitle(),
                        context.examMatrixId(),
                        context.matrixTitle(),
                        context.rows().stream()
                                .map(row -> new QuizGenerationContextView.Row(
                                        row.rowId(),
                                        row.learningOutcomeId(),
                                        row.learningOutcomeCode(),
                                        row.chapterTitle(),
                                        row.topic(),
                                        row.understandCount(),
                                        row.applyCount(),
                                        row.applyPlusCount()))
                                .toList()));
        } catch (IllegalStateException ex) {
            throw new InvalidQuizGenerationRequestException(ex.getMessage());
        }
    }

    @Override
    public Optional<String> findValidationError(UUID courseId, List<UUID> materialIds) {
        return materialSelectionApi.inspectReadyPdfMaterials(courseId, materialIds).stream()
                .filter(result -> !result.eligible())
                .findFirst()
                .map(result -> result.reason() + ": " + result.materialId());
    }

    @Override
    public boolean allBelongToCourse(UUID courseId, Collection<UUID> learningOutcomeIds) {
        return learningOutcomeCatalogApi.findExistingIds(courseId, learningOutcomeIds).size()
                == learningOutcomeIds.stream().distinct().count();
    }
}
