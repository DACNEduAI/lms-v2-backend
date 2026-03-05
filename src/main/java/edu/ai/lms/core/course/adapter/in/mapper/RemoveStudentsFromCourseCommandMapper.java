package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.RemoveStudentsFromCourseRequest;
import edu.ai.lms.core.course.app.port.in.command.RemoveStudentsFromCourseCommand;

@Component
public class RemoveStudentsFromCourseCommandMapper {

    public RemoveStudentsFromCourseCommand toCommand(UUID courseId, RemoveStudentsFromCourseRequest request) {
        return RemoveStudentsFromCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(request.getStudentEmails())
                .build();
    }
}
