package edu.ai.lms.core.course.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.request.CreateCourseRequest;
import edu.ai.lms.core.course.adapter.in.response.CreateCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateCourse;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadTeacherPort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;
import edu.ai.lms.core.course.exception.CourseCodeAlreadyExistsException;
import edu.ai.lms.core.course.exception.TeacherNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateCourseService implements TeacherCreateCourse {

    @Autowired
    private final SaveCoursePort saveCoursePort;
    private final LoadCoursePort loadCoursePort;
    private final LoadTeacherPort loadTeacherPort;

    @Override
    @Transactional
    public CreateCourseResponse createCourse(CreateCourseRequest request) {
        // Business rule validation: Check if entity course already exists
        if (loadCoursePort.findByCourseCode(request.getCourseCode()).isPresent()) {
            throw new CourseCodeAlreadyExistsException("Course code already exists: " + request.getCourseCode());
        }

        // Validate teacher exists
        Teacher teacher = loadTeacherPort.findById(request.getTeacherId())
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not found with id: " + request.getTeacherId()));

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
                .chapters(request.getChapters())
//                .learningOutcomes(request.getLearningOutcomes())
                .build();

        // Save the course
        Course savedCourse = saveCoursePort.save(course);

        // Return response
        return CreateCourseResponse.builder()
                .courseId(savedCourse.getId())
                .courseName(savedCourse.getCourseName())
                .courseCode(savedCourse.getCourseCode())
                .description(savedCourse.getDescription())
                .teacherId(request.getTeacherId())
                .active(savedCourse.getActive())
                .coverImage(savedCourse.getCoverImage())
                .startDate(savedCourse.getStartDate())
                .endDate(savedCourse.getEndDate())
                .build();
    }
}
