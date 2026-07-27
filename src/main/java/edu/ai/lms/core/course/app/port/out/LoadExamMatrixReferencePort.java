package edu.ai.lms.core.course.app.port.out;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CustomLearningLevel;

public interface LoadExamMatrixReferencePort {
    List<CustomLearningLevel> findCustomLearningLevelsByIds(Collection<UUID> ids);
}
