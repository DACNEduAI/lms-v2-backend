package edu.ai.lms.core.quiz.app.port.out;

import java.util.Collection;
import java.util.UUID;

public interface ValidateLearningOutcomesPort {

    boolean allBelongToCourse(UUID courseId, Collection<UUID> learningOutcomeIds);
}
