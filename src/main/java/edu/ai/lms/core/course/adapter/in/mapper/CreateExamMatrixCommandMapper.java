package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.CreateExamMatrixRequest;
import edu.ai.lms.core.course.adapter.in.request.ExamMatrixRowRequest;
import edu.ai.lms.core.course.app.port.in.command.*;

@Component
public class CreateExamMatrixCommandMapper {
    public CreateExamMatrixCommand toCommand(UUID courseId, CreateExamMatrixRequest request) {
        return CreateExamMatrixCommand.builder()
                .courseId(courseId).title(request.getTitle()).description(request.getDescription())
                .rows(request.getRows().stream().map(this::toRow).toList()).build();
    }

    ExamMatrixRowCommand toRow(ExamMatrixRowRequest row) {
        return ExamMatrixRowCommand.builder()
                .chapterId(row.getChapterId()).topic(row.getTopic()).orderIndex(row.getOrderIndex())
                .learningOutcomeIds(row.getLearningOutcomeIds())
                .levelCounts(row.getLevelCounts().stream().map(value -> LearningLevelCountCommand.builder()
                        .customLearningLevelId(value.getCustomLearningLevelId())
                        .questionCount(value.getQuestionCount()).build()).toList())
                .build();
    }
}
