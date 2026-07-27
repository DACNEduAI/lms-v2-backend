package edu.ai.lms.core.course.app.service.exammatrix;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.response.*;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.domain.ExamMatrixSummary;

@Component
class ExamMatrixResponseAssembler {
    ExamMatrixResponse toResponse(ExamMatrix matrix) {
        return ExamMatrixResponse.builder()
                .id(matrix.getId()).courseId(matrix.getCourseId()).title(matrix.getTitle())
                .description(matrix.getDescription()).createdAt(matrix.getCreatedAt())
                .updatedAt(matrix.getUpdatedAt())
                .rows(matrix.getRows().stream().map(row -> ExamMatrixRowResponse.builder()
                        .id(row.getId()).chapterId(row.getChapterId()).topic(row.getTopic())
                        .orderIndex(row.getOrderIndex())
                        .learningOutcomeIds(List.copyOf(row.getLearningOutcomeIds()))
                        .levelCounts(row.getLevelCounts().stream().map(count ->
                                LearningLevelCountResponse.builder()
                                        .customLearningLevelId(count.getCustomLearningLevelId())
                                        .learningOutcomeIds(List.copyOf(count.getLearningOutcomeIds()))
                                        .code(count.getCode()).name(count.getName()).description(count.getDescription())
                                        .sortOrder(count.getSortOrder())
                                        .questionCount(count.getQuestionCount()).build()).toList())
                        .build()).toList())
                .build();
    }

    ExamMatrixSummaryResponse toSummary(ExamMatrixSummary summary) {
        return ExamMatrixSummaryResponse.builder()
                .id(summary.getId()).title(summary.getTitle()).description(summary.getDescription())
                .rowCount(summary.getRowCount()).totalQuestionCount(summary.getTotalQuestionCount())
                .createdAt(summary.getCreatedAt()).updatedAt(summary.getUpdatedAt()).build();
    }
}
