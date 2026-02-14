package edu.ai.lms.core.course.app.port.in.command;

import edu.ai.lms.core.course.app.domain.Course;
import lombok.Builder;

@Builder
public record CreateCourseCommand(Course course) {
}
