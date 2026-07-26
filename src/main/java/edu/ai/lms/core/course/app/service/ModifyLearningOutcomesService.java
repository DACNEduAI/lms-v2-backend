package edu.ai.lms.core.course.app.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.ModifyLearningOutcomesResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ModifyLearningOutcomesCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherModifyLearningOutcomesUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModifyLearningOutcomesService implements TeacherModifyLearningOutcomesUseCase {

    private final SaveLearningOutcomePort saveLearningOutcomePort;
    private final LoadLearningOutcomePort loadLearningOutcomePort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public ModifyLearningOutcomesResponse modifyLearningOutcomes(ModifyLearningOutcomesCommand command) {
        // Verify teacher is authenticated
        CurrentActor user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        // Verify course exists and belongs to the authenticated teacher
        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to modify learning outcomes in this course");
        }

        // Modify each learning outcome
        List<ModifyLearningOutcomesResponse.LearningOutcomeDto> modifiedDtos = new ArrayList<>();
        for (LearningOutcome lo : command.learningOutcomes()) {
            // Verify each learning outcome exists
            LearningOutcome existing = loadLearningOutcomePort.findById(lo.getId())
                    .orElseThrow(() -> new LearningOutcomeNotFoundException(
                            "Learning outcome not found with id: " + lo.getId()));

            // Verify it belongs to this course
            if (!existing.getCourseId().equals(command.courseId())) {
                throw new IllegalArgumentException(
                        "Learning outcome " + lo.getId() + " does not belong to this course");
            }

            // Update fields
            LearningOutcome updated = LearningOutcome.builder()
                    .id(existing.getId())
                    .courseId(existing.getCourseId())
                    .groupLearningOutcomeId(existing.getGroupLearningOutcomeId())
                    .prefixNumber(lo.getPrefixNumber() != null ? lo.getPrefixNumber() : existing.getPrefixNumber())
                    .sortOrder(lo.getSortOrder() != null ? lo.getSortOrder() : existing.getSortOrder())
                    .description(lo.getDescription() != null ? lo.getDescription() : existing.getDescription())
                    .createdAt(existing.getCreatedAt())
                    .build();

            LearningOutcome saved = saveLearningOutcomePort.save(updated);
            modifiedDtos.add(ModifyLearningOutcomesResponse.LearningOutcomeDto.builder()
                    .id(saved.getId())
                    .code(saved.getCode())
                    .prefixNumber(saved.getPrefixNumber())
                    .sortOrder(saved.getSortOrder())
                    .displayCode(saved.getCode())
                    .description(saved.getDescription())
                    .updatedAt(saved.getUpdatedAt())
                    .build());
        }

        return ModifyLearningOutcomesResponse.builder()
                .courseId(course.getId())
                .learningOutcomes(modifiedDtos)
                .totalModified(modifiedDtos.size())
                .build();
    }
}
