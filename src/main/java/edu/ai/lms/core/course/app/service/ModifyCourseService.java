package edu.ai.lms.core.course.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.request.ModifyCourseRequest;
import edu.ai.lms.core.course.adapter.in.response.ModifyCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherModifyCourseUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInTeacherPort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;
import edu.ai.lms.core.course.exception.CourseNotFoundException;
import edu.ai.lms.core.course.exception.TeacherNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModifyCourseService implements TeacherModifyCourseUseCase {

    private final SaveCoursePort saveCoursePort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInTeacherPort loadSignedInTeacherPort;

    @Override
    @Transactional
    public ModifyCourseResponse modifyCourse(ModifyCourseRequest request) {
        // Validate course exists
        Course existingCourse = loadCoursePort.findById(request.getCourseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + request.getCourseId()));

        // Validate teacher exists and get authenticated teacher
        Teacher teacher = loadSignedInTeacherPort.findAuthenticatedTeacher()
                .orElseThrow(() -> new TeacherNotFoundException("Authenticated teacher not found"));

        // Business rule: Validate that the teacher owns the course
        if (!existingCourse.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to modify this course");
        }

        // Build the learning outcome list
        List<LearningOutcome> learningOutcomes = request.getLearningOutcomes().stream()
                .map(learningOutcomeRequest -> LearningOutcome.builder()
                        .code(learningOutcomeRequest.getCode())
                        .description(learningOutcomeRequest.getDescription())
                        .build())
                .collect(Collectors.toList());

        // Update the Course domain object
        Course updatedCourse = Course.builder()
                .id(existingCourse.getId())
                .courseName(request.getCourseName())
                .description(request.getDescription())
                .courseCode(request.getCourseCode())
                .teacher(teacher)
                .active(request.getActive())
                .coverImage(request.getCoverImage())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .learningOutcomes(learningOutcomes)
                .build();

        // Save the updated course
        Course savedCourse = saveCoursePort.save(updatedCourse);

        // Return response
        return ModifyCourseResponse.builder()
                .courseId(savedCourse.getId())
                .courseName(savedCourse.getCourseName())
                .courseCode(savedCourse.getCourseCode())
                .description(savedCourse.getDescription())
                .teacherId(teacher.getId())
                .active(savedCourse.getActive())
                .coverImage(savedCourse.getCoverImage())
                .startDate(savedCourse.getStartDate())
                .endDate(savedCourse.getEndDate())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
