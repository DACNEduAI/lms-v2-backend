package edu.ai.lms.core.course.app.service;

import java.util.List;
import java.util.stream.Collectors;

import edu.ai.lms.core.course.exception.TeacherNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.request.CreateCourseRequest;
import edu.ai.lms.core.course.adapter.in.response.CreateCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateCourse;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInTeacherPort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;
import edu.ai.lms.core.course.exception.CourseCodeAlreadyExistsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateCourseService implements TeacherCreateCourse {

    @Autowired
    private final SaveCoursePort saveCoursePort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInTeacherPort loadSignedInTeacherPort;

    @Override
    @Transactional
    public CreateCourseResponse createCourse(CreateCourseRequest request) {
        // Business rule validation: Check if entity course already exists
        if (loadCoursePort.findByCourseCode(request.getCourseCode()).isPresent()) {
            throw new CourseCodeAlreadyExistsException("Course code already exists: " + request.getCourseCode());
        }

        // Validate teacher exists using the token from the request
        Teacher teacher = loadSignedInTeacherPort.findAuthenticatedTeacher()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        // Build the learning outcome list
        List<LearningOutcome> learningOutcomes = request.getLearningOutcomes().stream()
                .map(learningOutcomeRequest -> LearningOutcome.builder()
                        .code(learningOutcomeRequest.getCode())
                        .description(learningOutcomeRequest.getDescription())
                        .build())
                .collect(Collectors.toList());

        // Build the Course domain object
        Course course = Course.builder()
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

        // Save the course
        Course savedCourse = saveCoursePort.save(course);

        // Return response
        return CreateCourseResponse.builder()
                .courseId(savedCourse.getId())
                .courseName(savedCourse.getCourseName())
                .courseCode(savedCourse.getCourseCode())
                .description(savedCourse.getDescription())
                .teacherId(teacher.getId())
                .active(savedCourse.getActive())
                .coverImage(savedCourse.getCoverImage())
                .startDate(savedCourse.getStartDate())
                .endDate(savedCourse.getEndDate())
                .build();
    }
}
