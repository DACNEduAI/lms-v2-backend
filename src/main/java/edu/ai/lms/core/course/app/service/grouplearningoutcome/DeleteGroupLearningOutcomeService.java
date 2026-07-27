package edu.ai.lms.core.course.app.service.grouplearningoutcome;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.DeleteGroupLearningOutcomeResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.GroupLearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.DeleteGroupLearningOutcomeCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherDeleteGroupLearningOutcomeUseCase;
import edu.ai.lms.core.course.app.port.out.DeleteLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveGroupLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteGroupLearningOutcomeService implements TeacherDeleteGroupLearningOutcomeUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadGroupLearningOutcomePort loadGroupLearningOutcomePort;
    private final SaveGroupLearningOutcomePort saveGroupLearningOutcomePort;
    private final DeleteLearningOutcomePort deleteLearningOutcomePort;

    @Override
    @Transactional
    public DeleteGroupLearningOutcomeResponse deleteGroupLearningOutcome(DeleteGroupLearningOutcomeCommand command) {
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
            throw new IllegalArgumentException("Group learning outcome does not belong to this course");
        }

        if (group.getLearningOutcomes() != null && !group.getLearningOutcomes().isEmpty()) {
            deleteLearningOutcomePort.deleteAllByIds(group.getLearningOutcomes().stream()
                    .map(learningOutcome -> learningOutcome.getId())
                    .toList());
        }
        saveGroupLearningOutcomePort.deleteById(group.getId(), command.courseId());
        return DeleteGroupLearningOutcomeResponse.builder()
                .courseId(command.courseId())
                .groupLearningOutcomeId(group.getId())
                .success(true)
                .message("Group learning outcome deleted successfully")
                .build();
    }
}
