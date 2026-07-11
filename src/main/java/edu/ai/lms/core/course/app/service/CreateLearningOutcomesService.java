package edu.ai.lms.core.course.app.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.adapter.in.response.CreateLearningOutcomesResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.GroupLearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.CreateLearningOutcomesCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateLearningOutcomesUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateLearningOutcomesService implements TeacherCreateLearningOutcomesUseCase {

    private final SaveLearningOutcomePort saveLearningOutcomePort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadGroupLearningOutcomePort loadGroupLearningOutcomePort;
    private final LoadLearningOutcomePort loadLearningOutcomePort;

    @Override
    @Transactional
    public CreateLearningOutcomesResponse createLearningOutcomes(CreateLearningOutcomesCommand command) {

        UserAuth user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        // Verify course exists and belongs to the authenticated teacher
        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to add learning outcomes to this course");
        }

        GroupLearningOutcome group = null;
        if (command.groupLearningOutcomeId() != null && loadGroupLearningOutcomePort != null) {
            group = loadGroupLearningOutcomePort.findById(command.groupLearningOutcomeId())
                    .orElseThrow(() -> new GroupLearningOutcomeNotFoundException(
                            "Group learning outcome not found with id: " + command.groupLearningOutcomeId()));

            if (!course.getId().equals(group.getCourseId())) {
                throw new IllegalArgumentException("Group learning outcome does not belong to this course");
            }
        }

        List<CreateLearningOutcomesResponse.LearningOutcomeDto> savedDtos = new ArrayList<>();
        int nextSortOrder = group == null || loadLearningOutcomePort == null
                ? 1
                : loadLearningOutcomePort.findMaxSortOrderByGroupId(group.getId()) + 1;
        for (LearningOutcome lo : command.learningOutcomes()) {
            lo.setCourseId(course.getId());
            if (group != null) {
                lo.setGroupLearningOutcomeId(group.getId());
                lo.setPrefixNumber(nextSortOrder);
                lo.setSortOrder(nextSortOrder);
            }
            LearningOutcome saved = saveLearningOutcomePort.save(lo);
            savedDtos.add(CreateLearningOutcomesResponse.LearningOutcomeDto.builder()
                    .id(saved.getId())
                    .code(saved.getCode())
                    .prefixNumber(saved.getPrefixNumber())
                    .sortOrder(saved.getSortOrder())
                    .displayCode(group == null ? saved.getCode() : group.getCode() + "." + saved.getPrefixNumber())
                    .description(saved.getDescription())
                    .createdAt(saved.getCreatedAt())
                    .build());
            nextSortOrder++;
        }

        return CreateLearningOutcomesResponse.builder()
                .courseId(course.getId())
                .learningOutcomes(savedDtos)
                .totalCreated(savedDtos.size())
                .build();
    }
}
