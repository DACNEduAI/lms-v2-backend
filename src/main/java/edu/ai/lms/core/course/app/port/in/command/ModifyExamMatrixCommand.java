package edu.ai.lms.core.course.app.port.in.command;

import java.util.List;
import java.util.UUID;

import lombok.Builder;

/**
 * Command for modifying an existing exam matrix.
 */
@Builder
public record ModifyExamMatrixCommand(
        UUID matrixId,
        UUID courseId,
        String title,
        String description,
        List<ExamMatrixRowCommand> rows
) {}
