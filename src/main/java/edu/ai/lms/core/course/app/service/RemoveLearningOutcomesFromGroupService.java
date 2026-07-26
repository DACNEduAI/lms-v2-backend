package edu.ai.lms.core.course.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.RemoveLearningOutcomesFromGroupResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.GroupLearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.RemoveLearningOutcomesFromGroupCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherRemoveLearningOutcomesFromGroupUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemoveLearningOutcomesFromGroupService implements TeacherRemoveLearningOutcomesFromGroupUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadGroupLearningOutcomePort loadGroupLearningOutcomePort;
    private final LoadLearningOutcomePort loadLearningOutcomePort;
    private final SaveLearningOutcomePort saveLearningOutcomePort;

    @Override
    @Transactional
    public RemoveLearningOutcomesFromGroupResponse removeLearningOutcomesFromGroup(RemoveLearningOutcomesFromGroupCommand command) {
        CurrentActor user = loadSignedInUserPort.findAuthenticatedUser()
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

        // Validate learning outcomes exist and belong to this group
        List<LearningOutcome> learningOutcomes = new ArrayList<>();
        for (UUID loId : command.learningOutcomeIds()) {
            LearningOutcome lo = loadLearningOutcomePort.findById(loId)
                    .orElseThrow(() -> new LearningOutcomeNotFoundException("Learning outcome not found with id: " + loId));

            if (lo.getGroupLearningOutcomeId() == null || !lo.getGroupLearningOutcomeId().equals(group.getId())) {
                throw new IllegalArgumentException("Learning outcome " + loId + " is not in group " + group.getId());
            }
            learningOutcomes.add(lo);
        }

        // Remove from group
        for (LearningOutcome lo : learningOutcomes) {
            saveLearningOutcomePort.removeFromGroup(lo.getId(), lo.getCourseId());
        }

        return RemoveLearningOutcomesFromGroupResponse.builder()
                .groupId(group.getId())
                .name(group.getName())
                .code(group.getCode())
                .courseId(group.getCourseId())
                .totalRemoved(learningOutcomes.size())
                .message("Successfully removed " + learningOutcomes.size() + " learning outcomes from group '" + group.getName() + "'")
                .build();
    }
}
