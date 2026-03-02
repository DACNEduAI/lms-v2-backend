package edu.ai.lms.core.course.app.port.out;

import java.util.List;
import java.util.UUID;

public interface DeleteLearningOutcomePort {
    void deleteById(UUID id);
    void deleteAllByIds(List<UUID> ids);
}
