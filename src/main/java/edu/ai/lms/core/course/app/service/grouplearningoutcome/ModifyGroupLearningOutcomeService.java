package edu.ai.lms.core.course.app.service.grouplearningoutcome;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.GetGroupLearningOutcomeResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.GroupLearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ModifyGroupLearningOutcomeCommand;
import edu.ai.lms.core.course.app.port.in.usecase.GetGroupLearningOutcomeUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherModifyGroupLearningOutcomeUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveGroupLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModifyGroupLearningOutcomeService implements TeacherModifyGroupLearningOutcomeUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadGroupLearningOutcomePort loadGroupLearningOutcomePort;
    private final SaveGroupLearningOutcomePort saveGroupLearningOutcomePort;
    private final GetGroupLearningOutcomeUseCase getGroupLearningOutcomeUseCase;

    @Override
    @Transactional
    public GetGroupLearningOutcomeResponse modifyGroupLearningOutcome(ModifyGroupLearningOutcomeCommand command) {
        CurrentActor user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));
        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));
        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to modify this course");
        }

        GroupLearningOutcome existing = loadGroupLearningOutcomePort.findById(command.groupLearningOutcomeId())
                .orElseThrow(() -> new GroupLearningOutcomeNotFoundException(
                        "Group learning outcome not found with id: " + command.groupLearningOutcomeId()));
        if (!existing.getCourseId().equals(command.courseId())) {
            throw new IllegalArgumentException("Group learning outcome does not belong to this course");
        }

        existing.setDescription(command.description() != null ? command.description() : existing.getDescription());
        saveGroupLearningOutcomePort.save(existing);

        return getGroupLearningOutcomeUseCase.getGroupedLearningOutcomes(command.courseId()).stream()
                .filter(group -> group.getId().equals(command.groupLearningOutcomeId()))
                .findFirst()
                .orElseThrow();
    }
}
