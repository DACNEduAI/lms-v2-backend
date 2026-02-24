package edu.ai.lms.core.course.app.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.GetCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.port.in.usecase.GetCourseUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInTeacherPort;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetCourseService implements GetCourseUseCase {

    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInTeacherPort loadSignedInTeacherPort;

    @Override
    @Transactional(readOnly = true)
    public GetCourseResponse getCourse(UUID courseId) {
        // Validate course exists
        Course course = loadCoursePort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + courseId));

        // Validate teacher exists and get authenticated teacher
        Teacher teacher = loadSignedInTeacherPort.findAuthenticatedTeacher()
                .orElseThrow(() -> new TeacherNotFoundException("Authenticated teacher not found"));

        // Business rule: Validate that the teacher is associated with the course
        if (!course.getTeacherIds().contains(teacher.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to access this course");
        }

        // Map learning outcomes
        List<GetCourseResponse.LearningOutcomeDto> learningOutcomeDtos = course.getLearningOutcomes().stream()
                .map(lo -> GetCourseResponse.LearningOutcomeDto.builder()
                        .id(lo.getId())
                        .code(lo.getCode())
                        .description(lo.getDescription())
                        .build())
                .collect(Collectors.toList());

        // Return response
        return GetCourseResponse.builder()
                .courseId(course.getId())
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .description(course.getDescription())
                .teacherId(teacher.getId())
                .active(course.getActive())
                .coverImage(course.getCoverImage())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .learningOutcomes(learningOutcomeDtos)
                .build();
    }
}
