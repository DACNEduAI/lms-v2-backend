package edu.ai.lms.core.course.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.UserAuth;
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
    private final LoadLearningOutcomePort loadLearningOutcomePort;
    private final SaveGroupLearningOutcomePort saveGroupLearningOutcomePort;
    private final SaveLearningOutcomePort saveLearningOutcomePort;

    @Override
    @Transactional
    public CreateGroupLearningOutcomeResponse createGroupLearningOutcome(CreateGroupLearningOutcomeCommand command) {
        UserAuth user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to modify this course");
        }

        // Validate learning outcomes exist and belong to this course
        List<LearningOutcome> learningOutcomes = new ArrayList<>();
        List<UUID> alreadyInGroup = new ArrayList<>();
        for (UUID loId : command.learningOutcomeIds()) {
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

        // Create and save the group
        GroupLearningOutcome group = GroupLearningOutcome.builder()
                .name(command.name())
                .description(command.description())
                .courseId(command.courseId())
                .build();

        GroupLearningOutcome savedGroup = saveGroupLearningOutcomePort.save(group);

        // Assign learning outcomes to the group
        for (LearningOutcome lo : learningOutcomes) {
            saveLearningOutcomePort.assignToGroup(lo.getId(), lo.getCourseId(), savedGroup.getId());
        }

        // Build response
        List<CreateGroupLearningOutcomeResponse.LearningOutcomeDto> loDtos = learningOutcomes.stream()
                .map(lo -> CreateGroupLearningOutcomeResponse.LearningOutcomeDto.builder()
                        .id(lo.getId())
                        .code(lo.getCode())
                        .description(lo.getDescription())
                        .build())
                .toList();

        return CreateGroupLearningOutcomeResponse.builder()
                .id(savedGroup.getId())
                .name(savedGroup.getName())
                .description(savedGroup.getDescription())
                .courseId(savedGroup.getCourseId())
                .learningOutcomes(loDtos)
                .createdAt(savedGroup.getCreatedAt())
                .build();
    }
}
