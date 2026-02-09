package edu.ai.lms.core.course.adapter.out.persistence;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.database.CourseJpaEntity;
import edu.ai.lms.database.LearningOutcomeJpaEntity;

@Component
public class LearningOutcomeMapper {

    public LearningOutcomeJpaEntity mapToEntity(LearningOutcome learningOutcome, CourseJpaEntity courseEntity) {
        return LearningOutcomeJpaEntity.builder()
                .id(learningOutcome.getId())
                .code(learningOutcome.getCode())
                .description(learningOutcome.getDescription())
                .course(courseEntity)
                .build();
    }

    public LearningOutcome mapToLearningOutcome(LearningOutcomeJpaEntity entity, Course course) {
        return LearningOutcome.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .description(entity.getDescription())
                .course(course)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
