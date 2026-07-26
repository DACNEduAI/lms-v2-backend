package edu.ai.lms.core.course.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.CreateGroupLearningOutcomeResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeAlreadyInGroupException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.CreateGroupLearningOutcomeCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateGroupLearningOutcomeUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateGroupLearningOutcomeService implements TeacherCreateGroupLearningOutcomeUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadGroupLearningOutcomePort loadGroupLearningOutcomePort;
    private final LoadLearningOutcomePort loadLearningOutcomePort;
    private final SaveGroupLearningOutcomePort saveGroupLearningOutcomePort;
    private final SaveLearningOutcomePort saveLearningOutcomePort;

    @Override
    @Transactional
    public CreateGroupLearningOutcomeResponse createGroupLearningOutcome(CreateGroupLearningOutcomeCommand command) {
        CurrentActor user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to modify this course");
        }

        List<LearningOutcome> learningOutcomes = new ArrayList<>();
        List<UUID> alreadyInGroup = new ArrayList<>();
        List<UUID> requestedLearningOutcomeIds = command.learningOutcomeIds() == null ? List.of() : command.learningOutcomeIds();
        for (UUID loId : requestedLearningOutcomeIds) {
            LearningOutcome lo = loadLearningOutcomePort.findById(loId)
                    .orElseThrow(() -> new LearningOutcomeNotFoundException("Learning outcome not found with id: " + loId));

            if (!lo.getCourseId().equals(command.courseId())) {
                throw new IllegalArgumentException("Learning outcome " + loId + " does not belong to course " + command.courseId());
            }

            if (lo.getGroupLearningOutcomeId() != null) {
                alreadyInGroup.add(loId);
            }
            learningOutcomes.add(lo);
        }

        if (!alreadyInGroup.isEmpty()) {
            throw new LearningOutcomeAlreadyInGroupException(
                    "Some learning outcomes are already assigned to a group. Remove them from the existing group first.",
                    alreadyInGroup);
        }

        int nextSortOrder = loadGroupLearningOutcomePort == null
                ? 1
                : loadGroupLearningOutcomePort.findMaxSortOrderByCourseId(command.courseId()) + 1;
        String groupCode = command.name() == null || command.name().isBlank()
                ? buildGroupCode(nextSortOrder)
                : command.name();
        GroupLearningOutcome group = GroupLearningOutcome.builder()
                .code(groupCode)
                .description(command.description() != null ? command.description() : groupCode)
                .sortOrder(nextSortOrder)
                .courseId(command.courseId())
                .build();

        GroupLearningOutcome savedGroup = saveGroupLearningOutcomePort.save(group);
        if (savedGroup.getCode() == null) {
            savedGroup.setCode(group.getCode());
        }
        if (savedGroup.getSortOrder() == null) {
            savedGroup.setSortOrder(group.getSortOrder());
        }

        // Assign learning outcomes to the group
        for (LearningOutcome lo : learningOutcomes) {
            saveLearningOutcomePort.assignToGroup(lo.getId(), lo.getCourseId(), savedGroup.getId());
        }

        // Build response
        List<CreateGroupLearningOutcomeResponse.LearningOutcomeDto> loDtos = learningOutcomes.stream()
                .map(lo -> CreateGroupLearningOutcomeResponse.LearningOutcomeDto.builder()
                        .id(lo.getId())
                        .code(lo.getCode())
                        .prefixNumber(lo.getPrefixNumber())
                        .sortOrder(lo.getSortOrder())
                        .displayCode(savedGroup.getCode() + "." + lo.getPrefixNumber())
                        .description(lo.getDescription())
                        .build())
                .toList();

        return CreateGroupLearningOutcomeResponse.builder()
                .id(savedGroup.getId())
                .name(savedGroup.getName())
                .code(savedGroup.getCode())
                .sortOrder(savedGroup.getSortOrder())
                .displayCode(savedGroup.getCode())
                .description(savedGroup.getDescription())
                .courseId(savedGroup.getCourseId())
                .learningOutcomes(loDtos)
                .createdAt(savedGroup.getCreatedAt())
                .build();
    }

    private String buildGroupCode(int sortOrder) {
        return "L.O." + sortOrder;
    }
}
