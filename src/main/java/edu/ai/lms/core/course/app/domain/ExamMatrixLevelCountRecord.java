package edu.ai.lms.core.course.app.domain;

import java.util.List;
import java.util.UUID;

public record ExamMatrixLevelCountRecord(
        UUID examMatrixRowId, UUID customLearningLevelId,
        String code, String name, String description, int sortOrder,
        ExamMatrixQuestionType questionType, int questionCount) {
    public ExamMatrixLevelCount toDomain(List<UUID> learningOutcomeIds) {
        return ExamMatrixLevelCount.builder()
                .customLearningLevelId(customLearningLevelId)
                .learningOutcomeIds(learningOutcomeIds)
                .code(code)
                .name(name).description(description).sortOrder(sortOrder)
                .questionType(questionType)
                .questionCount(questionCount).build();
    }
}
