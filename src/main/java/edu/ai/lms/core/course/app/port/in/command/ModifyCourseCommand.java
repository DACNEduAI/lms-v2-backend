package edu.ai.lms.core.course.app.port.in.command;

import edu.ai.lms.core.course.app.domain.Course;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ModifyCourseCommand(UUID courseId, Course course) {
}
