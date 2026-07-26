package edu.ai.lms.core.course.app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.DeleteLearningOutcomesResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.DeleteLearningOutcomesCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherDeleteLearningOutcomesUseCase;
import edu.ai.lms.core.course.app.port.out.DeleteLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteLearningOutcomesService implements TeacherDeleteLearningOutcomesUseCase {

    private final DeleteLearningOutcomePort deleteLearningOutcomePort;
    private final LoadLearningOutcomePort loadLearningOutcomePort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public DeleteLearningOutcomesResponse deleteLearningOutcomes(DeleteLearningOutcomesCommand command) {
        // Verify teacher is authenticated
        CurrentActor user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        // Verify course exists and belongs to the authenticated teacher
        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to delete learning outcomes in this course");
        }

        // Verify each learning outcome exists and belongs to this course
        List<UUID> idsToDelete = command.learningOutcomeIds();
        for (UUID loId : idsToDelete) {
            LearningOutcome existing = loadLearningOutcomePort.findById(loId)
                    .orElseThrow(() -> new LearningOutcomeNotFoundException(
                            "Learning outcome not found with id: " + loId));

            if (!existing.getCourseId().equals(command.courseId())) {
                throw new IllegalArgumentException(
                        "Learning outcome " + loId + " does not belong to this course");
            }
        }

        // Delete all
        deleteLearningOutcomePort.deleteAllByIds(idsToDelete);

        return DeleteLearningOutcomesResponse.builder()
                .courseId(course.getId())
                .deletedIds(idsToDelete)
                .totalDeleted(idsToDelete.size())
                .message("Learning outcomes deleted successfully")
                .build();
    }
}
