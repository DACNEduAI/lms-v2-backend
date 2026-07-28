package edu.ai.lms.core.course.app.service.course;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.ChangeCourseSettingResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ChangeCourseSettingCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherChangeCourseSettingUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangeCourseSettingService implements TeacherChangeCourseSettingUseCase {

    private final SaveCoursePort saveCoursePort;
    private final LoadCoursePort loadCoursePort;

    @Override
    @Transactional
    public ChangeCourseSettingResponse changeCourseSetting(ChangeCourseSettingCommand command) {
        // Load existing course
        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        // Update course settings (only non-null fields)
        if (command.courseName() != null) {
            course.setCourseName(command.courseName());
        }
        if (command.description() != null) {
            course.setDescription(command.description());
        }
        if (command.active() != null) {
            course.setActive(command.active());
        }
        if (command.coverImage() != null) {
            course.setCoverImage(command.coverImage());
        }
        if (command.startDate() != null) {
            course.setStartDate(command.startDate());
        }
        if (command.endDate() != null) {
            course.setEndDate(command.endDate());
        }

        // Save the updated course
        Course savedCourse = saveCoursePort.save(course);

        // Return response
        return ChangeCourseSettingResponse.builder()
                .courseId(savedCourse.getId())
                .courseName(savedCourse.getCourseName())
                .courseCode(savedCourse.getCourseCode())
                .description(savedCourse.getDescription())
                .active(savedCourse.getActive())
                .coverImage(savedCourse.getCoverImage())
                .startDate(savedCourse.getStartDate())
                .endDate(savedCourse.getEndDate())
                .build();
    }
}
