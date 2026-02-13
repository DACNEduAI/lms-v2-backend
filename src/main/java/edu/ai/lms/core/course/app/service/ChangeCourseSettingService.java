package edu.ai.lms.core.course.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.request.ChangeCourseSettingRequest;
import edu.ai.lms.core.course.adapter.in.response.ChangeCourseSettingResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherChangeCourseSettingUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangeCourseSettingService implements TeacherChangeCourseSettingUseCase {

    private final SaveCoursePort saveCoursePort;
    private final LoadCoursePort loadCoursePort;

    @Override
    @Transactional
    public ChangeCourseSettingResponse changeCourseSetting(ChangeCourseSettingRequest request) {
        // Load existing course
        Course course = loadCoursePort.findById(request.getCourseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + request.getCourseId()));

        // Update course settings (only non-null fields)
        if (request.getCourseName() != null) {
            course.setCourseName(request.getCourseName());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getActive() != null) {
            course.setActive(request.getActive());
        }
        if (request.getCoverImage() != null) {
            course.setCoverImage(request.getCoverImage());
        }
        if (request.getStartDate() != null) {
            course.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            course.setEndDate(request.getEndDate());
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
