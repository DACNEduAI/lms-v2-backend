package edu.ai.lms.core.course.app.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CustomLearningLevel;

public interface CustomLearningLevelPort {
    List<CustomLearningLevel> findAllByCourseId(UUID courseId);
    List<CustomLearningLevel> lockAllByCourseId(UUID courseId);
    Optional<CustomLearningLevel> findByCourseIdAndId(UUID courseId, UUID id);
    long countMatrixUsages(UUID id);
    long countIncompatibleMatrixUsages(UUID id, List<UUID> learningOutcomeIds);
    void insert(CustomLearningLevel level);
    void stageUpdate(UUID id, String temporaryValue);
    void update(CustomLearningLevel level);
    void replaceOutcomeMappings(UUID levelId, List<UUID> learningOutcomeIds);
    void delete(UUID id);
}
