package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.AddStudentsToCourseRequest;
import edu.ai.lms.core.course.app.port.in.command.AddStudentsToCourseCommand;

@Component
public class AddStudentsToCourseCommandMapper {

    public AddStudentsToCourseCommand toCommand(UUID courseId, AddStudentsToCourseRequest request) {
        return AddStudentsToCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(request.getStudentEmails())
                .build();
    }
}
