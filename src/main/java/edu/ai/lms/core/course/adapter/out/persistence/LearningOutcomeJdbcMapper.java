package edu.ai.lms.core.course.adapter.out.persistence;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.database.LearningOutcomeJdbcEntity;

import java.util.UUID;

@Component
public class LearningOutcomeJdbcMapper {

    public LearningOutcomeJdbcEntity mapToEntity(LearningOutcome learningOutcome, UUID courseId) {
        return LearningOutcomeJdbcEntity.builder()
                .id(learningOutcome.getId())
                .code(learningOutcome.getCode())
                .description(learningOutcome.getDescription())
                .courseId(courseId)
                .build();
    }

    public LearningOutcome mapToLearningOutcome(LearningOutcomeJdbcEntity entity) {
        return LearningOutcome.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
