package edu.ai.lms.core.course.adapter.in.api;

import edu.ai.lms.core.course.api.CourseAccessApi;
import edu.ai.lms.core.course.api.LearningOutcomeCatalogApi;
import edu.ai.lms.core.course.api.LearningOutcomeView;
import edu.ai.lms.core.course.api.QuizGenerationContext;
import edu.ai.lms.core.course.api.QuizGenerationContextApi;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class CourseApiAdapter implements CourseAccessApi, QuizGenerationContextApi, LearningOutcomeCatalogApi {

    private final LoadCoursePort coursePort;
    private final LoadExamMatrixPort examMatrixPort;
    private final LoadLearningOutcomePort learningOutcomePort;
    private final LoadGroupLearningOutcomePort groupLearningOutcomePort;
    private final LoadChapterPort chapterPort;

    @Override
    public boolean isTeacherInCourse(UUID teacherId, UUID courseId) {
        return coursePort.findById(courseId)
                .map(course -> course.getTeacherIds().contains(teacherId))
                .orElse(false);
    }

    @Override
    public boolean isStudentInCourse(UUID studentId, UUID courseId) {
        return coursePort.findById(courseId)
                .map(course -> course.getStudentIds().contains(studentId))
                .orElse(false);
    }

    @Override
    public List<UUID> getStudentIds(UUID courseId) {
        return coursePort.findById(courseId)
                .map(course -> List.copyOf(course.getStudentIds()))
                .orElseGet(List::of);
    }

    @Override
    public Optional<QuizGenerationContext> load(UUID courseId, UUID examMatrixId) {
        var course = coursePort.findById(courseId).orElse(null);
        var matrix = examMatrixPort.findById(examMatrixId)
                .filter(value -> courseId.equals(value.getCourseId()))
                .orElse(null);
        if (course == null || matrix == null) {
            return Optional.empty();
        }
        List<QuizGenerationContext.QuizGenerationRow> rows = matrix.getRows().stream()
                .map(row -> {
                    LearningOutcome outcome = learningOutcomePort.findById(row.getLearningOutcomeId())
                            .filter(value -> courseId.equals(value.getCourseId()))
                            .orElseThrow(() -> new IllegalStateException("Matrix learning outcome is invalid"));
                    String chapterTitle = row.getChapterId() == null ? "" : chapterPort
                            .findByKey(courseId, row.getChapterId())
                            .map(chapter -> chapter.getTitle())
                            .orElseThrow(() -> new IllegalStateException("Matrix chapter is invalid"));
                    return new QuizGenerationContext.QuizGenerationRow(
                            row.getId(),
                            outcome.getId(),
                            formatLearningOutcomeCode(courseId, outcome),
                            chapterTitle == null ? "" : chapterTitle,
                            row.getTopic() == null ? "" : row.getTopic(),
                            row.getUnderstandCount(),
                            row.getApplyCount(),
                            row.getApplyPlusCount());
                })
                .toList();
        return Optional.of(new QuizGenerationContext(
                courseId,
                course.getCourseName(),
                matrix.getId(),
                matrix.getTitle(),
                rows));
    }

    @Override
    public Set<UUID> findExistingIds(UUID courseId, Collection<UUID> learningOutcomeIds) {
        if (learningOutcomeIds == null || learningOutcomeIds.isEmpty()) {
            return Set.of();
        }
        Set<UUID> result = new LinkedHashSet<>();
        for (UUID id : learningOutcomeIds) {
            learningOutcomePort.findById(id)
                    .filter(outcome -> courseId.equals(outcome.getCourseId()))
                    .ifPresent(outcome -> result.add(outcome.getId()));
        }
        return Set.copyOf(result);
    }

    @Override
    public Set<UUID> findExistingGroupIds(UUID courseId, Collection<UUID> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Set.of();
        }
        Set<UUID> result = new LinkedHashSet<>();
        groupIds.forEach(id -> groupLearningOutcomePort.findById(id)
                .filter(group -> courseId.equals(group.getCourseId()))
                .ifPresent(group -> result.add(group.getId())));
        return Set.copyOf(result);
    }

    @Override
    public List<LearningOutcomeView> findByIds(UUID courseId, Collection<UUID> learningOutcomeIds) {
        if (learningOutcomeIds == null || learningOutcomeIds.isEmpty()) {
            return List.of();
        }
        return learningOutcomeIds.stream()
                .distinct()
                .map(learningOutcomePort::findById)
                .flatMap(Optional::stream)
                .filter(outcome -> courseId.equals(outcome.getCourseId()))
                .map(this::toView)
                .toList();
    }

    @Override
    public List<LearningOutcomeView> findByGroupIds(UUID courseId, Collection<UUID> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> acceptedGroups = findExistingGroupIds(courseId, groupIds);
        return learningOutcomePort.findByCourseId(courseId).stream()
                .filter(outcome -> outcome.getGroupLearningOutcomeId() != null)
                .filter(outcome -> acceptedGroups.contains(outcome.getGroupLearningOutcomeId()))
                .map(this::toView)
                .toList();
    }

    private LearningOutcomeView toView(LearningOutcome outcome) {
        GroupLearningOutcome group = outcome.getGroupLearningOutcomeId() == null
                ? null
                : groupLearningOutcomePort.findById(outcome.getGroupLearningOutcomeId()).orElse(null);
        String code = group == null
                ? outcome.getCode()
                : group.getCode() + "." + outcome.getCode();
        return new LearningOutcomeView(
                outcome.getId(),
                code,
                outcome.getPrefixNumber(),
                outcome.getDescription(),
                group == null ? null : group.getId(),
                group == null ? null : group.getCode(),
                group == null ? null : group.getDescription());
    }

    private String formatLearningOutcomeCode(UUID courseId, LearningOutcome outcome) {
        String outcomeCode = outcome.getCode();
        if (outcome.getGroupLearningOutcomeId() == null) {
            return outcomeCode;
        }
        GroupLearningOutcome group = groupLearningOutcomePort.findById(outcome.getGroupLearningOutcomeId())
                .filter(value -> courseId.equals(value.getCourseId()))
                .orElseThrow(() -> new IllegalStateException("Matrix learning outcome group is invalid"));
        String groupCode = group.getCode();
        if (groupCode == null || groupCode.isBlank()) {
            return outcomeCode;
        }
        if (outcomeCode == null || outcomeCode.isBlank()) {
            return groupCode;
        }
        return groupCode.endsWith(".") ? groupCode + outcomeCode : groupCode + "." + outcomeCode;
    }
}
