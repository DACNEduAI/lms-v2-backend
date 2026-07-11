package edu.ai.lms.core.course.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.adapter.in.response.ModifyLearningOutcomeResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ModifySingleLearningOutcomeCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherModifySingleLearningOutcomeUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModifySingleLearningOutcomeService implements TeacherModifySingleLearningOutcomeUseCase {

    private final SaveLearningOutcomePort saveLearningOutcomePort;
    private final LoadLearningOutcomePort loadLearningOutcomePort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public ModifyLearningOutcomeResponse modifyLearningOutcome(ModifySingleLearningOutcomeCommand command) {
        // Verify teacher is authenticated
        UserAuth user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        // Verify course exists and belongs to the authenticated teacher
        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to modify learning outcomes in this course");
        }

        // Verify learning outcome exists
        LearningOutcome existing = loadLearningOutcomePort.findById(command.learningOutcomeId())
                .orElseThrow(() -> new LearningOutcomeNotFoundException(
                        "Learning outcome not found with id: " + command.learningOutcomeId()));

        // Verify learning outcome belongs to this course
        if (!existing.getCourseId().equals(command.courseId())) {
            throw new IllegalArgumentException("Learning outcome does not belong to this course");
        }

        // Update fields
        LearningOutcome updated = LearningOutcome.builder()
                .id(existing.getId())
                .courseId(existing.getCourseId())
                .groupLearningOutcomeId(existing.getGroupLearningOutcomeId())
                .prefixNumber(command.learningOutcome().getPrefixNumber() != null
                        ? command.learningOutcome().getPrefixNumber()
                        : existing.getPrefixNumber())
                .sortOrder(command.learningOutcome().getSortOrder() != null
                        ? command.learningOutcome().getSortOrder()
                        : existing.getSortOrder())
                .description(command.learningOutcome().getDescription() != null
                        ? command.learningOutcome().getDescription()
                        : existing.getDescription())
                .createdAt(existing.getCreatedAt())
                .build();

        LearningOutcome saved = saveLearningOutcomePort.save(updated);

        return ModifyLearningOutcomeResponse.builder()
                .id(saved.getId())
                .code(saved.getCode())
                .prefixNumber(saved.getPrefixNumber())
                .sortOrder(saved.getSortOrder())
                .displayCode(saved.getCode())
                .description(saved.getDescription())
                .courseId(saved.getCourseId())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
}
