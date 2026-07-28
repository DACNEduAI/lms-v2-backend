package edu.ai.lms.core.course.app.domain;

import java.util.UUID;

public record ExamMatrixLevelCountOutcomeLink(
        UUID examMatrixRowId,
        UUID customLearningLevelId,
        UUID learningOutcomeId) {
}
