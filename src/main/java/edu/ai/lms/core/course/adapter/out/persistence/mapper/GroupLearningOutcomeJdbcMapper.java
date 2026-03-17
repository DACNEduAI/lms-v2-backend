package edu.ai.lms.core.course.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.database.GroupLearningOutcomeJdbcTable;

import java.util.UUID;

@Component
public class GroupLearningOutcomeJdbcMapper {

    public GroupLearningOutcomeJdbcTable mapToEntity(GroupLearningOutcome domain, UUID courseId) {
        return GroupLearningOutcomeJdbcTable.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .courseId(courseId)
                .build();
    }

    public GroupLearningOutcome mapToDomain(GroupLearningOutcomeJdbcTable entity) {
        return GroupLearningOutcome.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .courseId(entity.getCourseId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
