package edu.ai.lms.core.course.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.course.adapter.in.response.AddLearningOutcomesToGroupResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.GroupLearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeAlreadyInGroupException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.AddLearningOutcomesToGroupCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherAddLearningOutcomesToGroupUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddLearningOutcomesToGroupService implements TeacherAddLearningOutcomesToGroupUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadGroupLearningOutcomePort loadGroupLearningOutcomePort;
    private final LoadLearningOutcomePort loadLearningOutcomePort;
    private final SaveLearningOutcomePort saveLearningOutcomePort;

    @Override
    @Transactional
    public AddLearningOutcomesToGroupResponse addLearningOutcomesToGroup(AddLearningOutcomesToGroupCommand command) {
        User user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to modify this course");
        }

        GroupLearningOutcome group = loadGroupLearningOutcomePort.findById(command.groupLearningOutcomeId())
                .orElseThrow(() -> new GroupLearningOutcomeNotFoundException(
                        "Group learning outcome not found with id: " + command.groupLearningOutcomeId()));

        if (!group.getCourseId().equals(command.courseId())) {
            throw new IllegalArgumentException("Group learning outcome does not belong to course " + command.courseId());
        }

        // Validate and collect learning outcomes
        List<LearningOutcome> learningOutcomes = new ArrayList<>();
        List<UUID> alreadyInGroup = new ArrayList<>();
        for (UUID loId : command.learningOutcomeIds()) {
            LearningOutcome lo = loadLearningOutcomePort.findById(loId)
                    .orElseThrow(() -> new LearningOutcomeNotFoundException("Learning outcome not found with id: " + loId));

            if (!lo.getCourseId().equals(command.courseId())) {
                throw new IllegalArgumentException("Learning outcome " + loId + " does not belong to course " + command.courseId());
            }

            if (lo.getGroupLearningOutcomeId() != null && !lo.getGroupLearningOutcomeId().equals(command.groupLearningOutcomeId())) {
                alreadyInGroup.add(loId);
            }
            learningOutcomes.add(lo);
        }

        if (!alreadyInGroup.isEmpty()) {
            throw new LearningOutcomeAlreadyInGroupException(
                    "Some learning outcomes are already assigned to another group. Remove them from the existing group first.",
                    alreadyInGroup);
        }

        // Assign learning outcomes to the group
        for (LearningOutcome lo : learningOutcomes) {
            saveLearningOutcomePort.assignToGroup(lo.getId(), lo.getCourseId(), group.getId());
        }

        // Build response with all LOs now in the group
        GroupLearningOutcome updatedGroup = loadGroupLearningOutcomePort.findById(group.getId())
                .orElseThrow(() -> new GroupLearningOutcomeNotFoundException("Group not found after update"));

        List<AddLearningOutcomesToGroupResponse.LearningOutcomeDto> allLoDtos = updatedGroup.getLearningOutcomes().stream()
                .map(lo -> AddLearningOutcomesToGroupResponse.LearningOutcomeDto.builder()
                        .id(lo.getId())
                        .code(lo.getCode())
                        .description(lo.getDescription())
                        .build())
                .toList();

        return AddLearningOutcomesToGroupResponse.builder()
                .groupId(group.getId())
                .name(group.getName())
                .courseId(group.getCourseId())
                .learningOutcomes(allLoDtos)
                .totalAdded(learningOutcomes.size())
                .build();
    }
}
