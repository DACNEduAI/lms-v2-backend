package edu.ai.lms.core.course.app.domain;

import java.util.UUID;

public record ExamMatrixRowOutcomeLink(UUID examMatrixRowId, UUID learningOutcomeId, boolean selected) {
}
