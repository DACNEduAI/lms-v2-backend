package edu.ai.lms.core.course.app.port.in.command;

import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record ChangeCourseSettingCommand(
        UUID courseId,
        String courseName,
        String description,
        Boolean active,
        String coverImage,
        LocalDate startDate,
        LocalDate endDate
) {
}
