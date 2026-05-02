package edu.ai.lms.core.notification.adapter.out.persistence;

import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.notification.app.port.out.LoadCourseStudentsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CourseNotificationAdapter implements LoadCourseStudentsPort {

    private final LoadCoursePort loadCoursePort;

    @Override
    public List<UUID> findStudentIdsByCourseId(UUID courseId) {
        return loadCoursePort.findById(courseId)
                .map(course -> course.getStudentIds())
                .orElse(Collections.emptyList());
    }
}
