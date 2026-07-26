package edu.ai.lms.core.course.app.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.GetGroupLearningOutcomeResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.GroupLearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ReorderLearningOutcomeTreeCommand;
import edu.ai.lms.core.course.app.port.in.usecase.GetGroupLearningOutcomeUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherReorderLearningOutcomeTreeUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReorderLearningOutcomeTreeService implements TeacherReorderLearningOutcomeTreeUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadGroupLearningOutcomePort loadGroupLearningOutcomePort;
    private final LoadLearningOutcomePort loadLearningOutcomePort;
    private final SaveGroupLearningOutcomePort saveGroupLearningOutcomePort;
    private final SaveLearningOutcomePort saveLearningOutcomePort;
    private final GetGroupLearningOutcomeUseCase getGroupLearningOutcomeUseCase;

    @Override
    @Transactional
    public List<GetGroupLearningOutcomeResponse> reorderLearningOutcomeTree(ReorderLearningOutcomeTreeCommand command) {
        verifyTeacherOwnsCourse(command.courseId());

        Map<UUID, GroupLearningOutcome> groupsById = loadGroupLearningOutcomePort.findByCourseId(command.courseId()).stream()
                .collect(Collectors.toMap(GroupLearningOutcome::getId, Function.identity()));
        Map<UUID, LearningOutcome> outcomesById = loadLearningOutcomePort.findByCourseId(command.courseId()).stream()
                .collect(Collectors.toMap(LearningOutcome::getId, Function.identity()));

        Set<UUID> seenGroups = new HashSet<>();
        Set<UUID> seenOutcomes = new HashSet<>();

        int groupSortOrder = 1;
        for (ReorderLearningOutcomeTreeCommand.GroupItem groupItem : command.groups()) {
            UUID groupId = groupItem.groupLearningOutcomeId();
            GroupLearningOutcome group = groupsById.get(groupId);
            if (group == null) {
                throw new GroupLearningOutcomeNotFoundException("Group learning outcome not found with id: " + groupId);
            }
            if (!seenGroups.add(groupId)) {
                throw new IllegalArgumentException("Group appears more than once in reorder payload: " + groupId);
            }

            String groupCode = "L.O." + groupSortOrder;
            saveGroupLearningOutcomePort.updateTreePosition(groupId, command.courseId(), groupCode, groupSortOrder);

            int childSortOrder = 1;
            List<UUID> learningOutcomeIds = groupItem.learningOutcomeIds() == null ? List.of() : groupItem.learningOutcomeIds();
            for (UUID learningOutcomeId : learningOutcomeIds) {
                LearningOutcome learningOutcome = outcomesById.get(learningOutcomeId);
                if (learningOutcome == null) {
                    throw new LearningOutcomeNotFoundException("Learning outcome not found with id: " + learningOutcomeId);
                }
                if (!seenOutcomes.add(learningOutcomeId)) {
                    throw new IllegalArgumentException("Learning outcome appears more than once in reorder payload: " + learningOutcomeId);
                }

                saveLearningOutcomePort.updateTreePosition(
                        learningOutcomeId,
                        command.courseId(),
                        groupId,
                        childSortOrder,
                        childSortOrder);
                childSortOrder++;
            }
            groupSortOrder++;
        }

        if (seenGroups.size() != groupsById.size()) {
            throw new IllegalArgumentException("Reorder payload must include every group learning outcome in the course");
        }
        if (seenOutcomes.size() != outcomesById.size()) {
            throw new IllegalArgumentException("Reorder payload must include every child learning outcome in the course");
        }

        return getGroupLearningOutcomeUseCase.getGroupedLearningOutcomes(command.courseId());
    }

    private void verifyTeacherOwnsCourse(UUID courseId) {
        CurrentActor user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));
        Course course = loadCoursePort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + courseId));
        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to modify this course");
        }
    }
}
