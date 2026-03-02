package edu.ai.lms.core.course.app.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.course.adapter.in.response.CreateLearningOutcomesResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.CreateLearningOutcomesCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateLearningOutcomesUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateLearningOutcomesService implements TeacherCreateLearningOutcomesUseCase {

    private final SaveLearningOutcomePort saveLearningOutcomePort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public CreateLearningOutcomesResponse createLearningOutcomes(CreateLearningOutcomesCommand command) {
        // Verify teacher is authenticated
        User user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        // Verify course exists and belongs to the authenticated teacher
        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to add learning outcomes to this course");
        }

        // Save each learning outcome
        List<CreateLearningOutcomesResponse.LearningOutcomeDto> savedDtos = new ArrayList<>();
        for (LearningOutcome lo : command.learningOutcomes()) {
            lo.setCourseId(course.getId());
            LearningOutcome saved = saveLearningOutcomePort.save(lo);
            savedDtos.add(CreateLearningOutcomesResponse.LearningOutcomeDto.builder()
                    .id(saved.getId())
                    .code(saved.getCode())
                    .description(saved.getDescription())
                    .createdAt(saved.getCreatedAt())
                    .build());
        }

        return CreateLearningOutcomesResponse.builder()
                .courseId(course.getId())
                .learningOutcomes(savedDtos)
                .totalCreated(savedDtos.size())
                .build();
    }
}
