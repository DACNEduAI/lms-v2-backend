package edu.ai.lms.core.course.app.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.ModifyCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ModifyCourseCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherModifyCourseUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInTeacherPort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModifyCourseService implements TeacherModifyCourseUseCase {

    private final SaveCoursePort saveCoursePort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInTeacherPort loadSignedInTeacherPort;

    @Override
    @Transactional
    public ModifyCourseResponse modifyCourse(ModifyCourseCommand command) {
        Course course = command.course();

        // Validate course exists
        Course existingCourse = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        // Validate teacher exists and get authenticated teacher
        Teacher teacher = loadSignedInTeacherPort.findAuthenticatedTeacher()
                .orElseThrow(() -> new TeacherNotFoundException("Authenticated teacher not found"));

        // Business rule: Validate that the teacher owns the course
        if (!existingCourse.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to modify this course");
        }

        // Update the Course domain object
        Course updatedCourse = Course.builder()
                .id(existingCourse.getId())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .courseCode(course.getCourseCode())
                .teacher(teacher)
                .active(course.getActive())
                .coverImage(course.getCoverImage())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .learningOutcomes(course.getLearningOutcomes())
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
