package edu.ai.lms.core.course.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.database.LearningOutcomeJdbcTable;

import java.util.UUID;

@Component
public class LearningOutcomeJdbcMapper {

    public LearningOutcomeJdbcTable mapToEntity(LearningOutcome learningOutcome, UUID courseId) {
        return LearningOutcomeJdbcTable.builder()
                .id(learningOutcome.getId())
                .code(learningOutcome.getCode())
                .description(learningOutcome.getDescription())
                .courseId(courseId)
                .build();
    }

    public LearningOutcome mapToLearningOutcome(LearningOutcomeJdbcTable entity) {
        return LearningOutcome.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
