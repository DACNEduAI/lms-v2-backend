package edu.ai.lms.core.course.app.port.in.command;

import java.util.List;
import java.util.UUID;

import lombok.Builder;

/**
 * Command for creating a new exam matrix under a course.
 */
@Builder
public record CreateExamMatrixCommand(
        UUID courseId,
        String title,
        String description,
        List<ExamMatrixRowCommand> rows
) {}
