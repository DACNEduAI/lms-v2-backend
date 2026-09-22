package edu.ai.lms.core.course.adapter.in.api;

import edu.ai.lms.core.course.api.CourseAccessApi;
import edu.ai.lms.core.course.api.CourseChatMetadata;
import edu.ai.lms.core.course.api.CourseChatMetadataApi;
import edu.ai.lms.core.course.api.LearningOutcomeCatalogApi;
import edu.ai.lms.core.course.api.LearningOutcomeView;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
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
class CourseApiAdapter implements CourseAccessApi, CourseChatMetadataApi, LearningOutcomeCatalogApi {

    private final LoadCoursePort coursePort;
    private final LoadLearningOutcomePort learningOutcomePort;
    private final LoadGroupLearningOutcomePort groupLearningOutcomePort;

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
    public Optional<CourseChatMetadata> findForChat(UUID courseId) {
        return coursePort.findById(courseId).map(course -> new CourseChatMetadata(
                course.getId(),
                course.getCourseCode(),
                course.getCourseName(),
                course.getDescription(),
                findByCourseId(courseId)));
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
    public List<LearningOutcomeView> findByCourseId(UUID courseId) {
        return learningOutcomePort.findByCourseId(courseId).stream()
                .map(this::toView)
                .sorted(java.util.Comparator.comparingLong(LearningOutcomeView::ktLearningOutcomeId))
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
                outcome.getKtLearningOutcomeId(),
                code,
                outcome.getPrefixNumber(),
                outcome.getDescription(),
                group == null ? null : group.getId(),
                group == null ? null : group.getCode(),
                group == null ? null : group.getDescription());
    }

}
