package edu.ai.lms.core.course.app.port.in.command;

import java.util.UUID;

import lombok.Builder;

/**
 * Command value-object for a single exam matrix row.
 * Shared between CreateExamMatrixCommand and ModifyExamMatrixCommand.
 */
@Builder
public record ExamMatrixRowCommand(
        UUID id,
        UUID learningOutcomeId,
        UUID chapterId,
        String topic,
        int understandCount,
        int applyCount,
        int applyPlusCount,
        int orderIndex
) {}
