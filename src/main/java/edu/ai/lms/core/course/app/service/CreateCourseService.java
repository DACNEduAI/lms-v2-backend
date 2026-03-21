package edu.ai.lms.core.course.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.adapter.in.response.CreateCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseCodeAlreadyExistsException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.CreateCourseCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateCourseUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateCourseService implements TeacherCreateCourseUseCase {

    private final SaveCoursePort saveCoursePort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public CreateCourseResponse createCourse(CreateCourseCommand command) {
        Course course = command.course();

        // Business rule validation: Check if entity course already exists
        if (loadCoursePort.findByCourseCode(course.getCourseCode()).isPresent()) {
            throw new CourseCodeAlreadyExistsException("Course code already exists: " + course.getCourseCode());
        }

        // Validate teacher exists using the token from the request
        UserAuth user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        // Add the authenticated teacher to the course's teacher list
        course.setTeacherIds(List.of(user.getId()));

        // Save the course
        Course savedCourse = saveCoursePort.save(course);

        // Return response
        return CreateCourseResponse.builder()
                .courseId(savedCourse.getId())
                .courseName(savedCourse.getCourseName())
                .courseCode(savedCourse.getCourseCode())
                .description(savedCourse.getDescription())
                .teacherId(user.getId())
                .active(savedCourse.getActive())
                .coverImage(savedCourse.getCoverImage())
                .startDate(savedCourse.getStartDate())
                .endDate(savedCourse.getEndDate())
                .build();
    }
}
