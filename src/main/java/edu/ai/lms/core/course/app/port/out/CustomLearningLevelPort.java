package edu.ai.lms.core.course.app.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CustomLearningLevel;

public interface CustomLearningLevelPort {
    List<CustomLearningLevel> findAllByMatrixId(UUID matrixId);
    List<CustomLearningLevel> lockAllByMatrixId(UUID matrixId);
    Optional<CustomLearningLevel> findByMatrixIdAndId(UUID matrixId, UUID id);
    void insert(CustomLearningLevel level);
    void stageUpdate(UUID id, String temporaryValue);
    void update(CustomLearningLevel level);
    void replaceOutcomeMappings(UUID levelId, List<UUID> learningOutcomeIds);
    void addUnselectedOutcomesToCourseMatrices(UUID courseId, List<UUID> learningOutcomeIds);
    void delete(UUID id);
}
