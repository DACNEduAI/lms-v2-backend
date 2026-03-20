package edu.ai.lms.core.course.adapter.in.mapper;

import edu.ai.lms.core.course.adapter.in.request.ModifyCourseRequest;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.port.in.command.ModifyCourseCommand;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ModifyCourseCommandMapper {

    public ModifyCourseCommand toCommand(UUID courseId, ModifyCourseRequest request) {
        Course course = Course.builder()
                .courseName(request.getCourseName())
                .description(request.getDescription())
                .courseCode(request.getCourseCode())
                .active(request.getActive())
                .coverImage(request.getCoverImage())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        return ModifyCourseCommand.builder()
                .courseId(courseId)
                .course(course)
                .build();
    }
}
