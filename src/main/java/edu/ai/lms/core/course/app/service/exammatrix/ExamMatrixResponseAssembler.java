package edu.ai.lms.core.course.app.service.exammatrix;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.response.*;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.domain.ExamMatrixSummary;

@Component
class ExamMatrixResponseAssembler {
    ExamMatrixResponse toResponse(ExamMatrix matrix) {
        List<UUID> emptyLevelIds = matrix.getCustomLearningLevels().stream()
                .filter(level -> level.getLearningOutcomeIds().isEmpty())
                .map(level -> level.getId()).toList();
        List<UUID> invalidRowIds = matrix.getRows().stream()
                .filter(row -> row.getLearningOutcomeIds().isEmpty())
                .map(row -> row.getId()).toList();
        boolean complete = matrix.getCustomLearningLevels().size() >= 3
                && emptyLevelIds.isEmpty()
                && invalidRowIds.isEmpty()
                && matrix.getUncoveredLearningOutcomeIds().isEmpty()
                && matrix.getCcctProfileId() != null
                && matrix.getRows().stream().allMatch(row ->
                        row.getChapterId() != null
                                && row.getCcctItemId() != null
                                && row.getLevelCounts().stream().allMatch(count ->
                                        count.getQuestionType()
                                                == edu.ai.lms.core.course.app.domain.ExamMatrixQuestionType.MULTIPLE_CHOICE));
        return ExamMatrixResponse.builder()
                .id(matrix.getId()).courseId(matrix.getCourseId())
                .ccctProfileId(matrix.getCcctProfileId()).title(matrix.getTitle())
                .description(matrix.getDescription()).createdAt(matrix.getCreatedAt())
                .updatedAt(matrix.getUpdatedAt())
                .customLearningLevels(matrix.getCustomLearningLevels().stream().map(level ->
                        CustomLearningLevelResponse.builder()
                                .id(level.getId())
                                .learningOutcomeIds(List.copyOf(level.getLearningOutcomeIds()))
                                .code(level.getCode()).name(level.getName())
                                .description(level.getDescription()).sortOrder(level.getSortOrder())
                                .createdAt(level.getCreatedAt()).updatedAt(level.getUpdatedAt()).build())
                        .toList())
                .configurationStatus(complete ? "COMPLETE" : "INCOMPLETE")
                .uncoveredLearningOutcomeIds(List.copyOf(matrix.getUncoveredLearningOutcomeIds()))
                .emptyCustomLearningLevelIds(emptyLevelIds)
                .invalidRowIds(invalidRowIds)
                .rows(matrix.getRows().stream().map(row -> ExamMatrixRowResponse.builder()
                        .id(row.getId()).chapterId(row.getChapterId())
                        .ccctItemId(row.getCcctItemId()).topic(row.getTopic())
                        .orderIndex(row.getOrderIndex())
                        .learningOutcomeIds(List.copyOf(row.getLearningOutcomeIds()))
                        .levelCounts(row.getLevelCounts().stream().map(count ->
                                LearningLevelCountResponse.builder()
                                        .customLearningLevelId(count.getCustomLearningLevelId())
                                        .learningOutcomeIds(List.copyOf(count.getLearningOutcomeIds()))
                                        .code(count.getCode()).name(count.getName()).description(count.getDescription())
                                        .sortOrder(count.getSortOrder())
                                        .questionType(count.getQuestionType())
                                        .questionCount(count.getQuestionCount())
                                        .active(count.isActive()).build()).toList())
                        .build()).toList())
                .build();
    }

    ExamMatrixSummaryResponse toSummary(ExamMatrixSummary summary) {
        return ExamMatrixSummaryResponse.builder()
                .id(summary.getId()).title(summary.getTitle()).description(summary.getDescription())
                .rowCount(summary.getRowCount()).totalQuestionCount(summary.getTotalQuestionCount())
                .configurationStatus(summary.getConfigurationStatus())
                .createdAt(summary.getCreatedAt()).updatedAt(summary.getUpdatedAt()).build();
    }
}
