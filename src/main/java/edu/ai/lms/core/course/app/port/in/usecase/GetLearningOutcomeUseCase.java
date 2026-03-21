package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.course.adapter.in.response.GetLearningOutcomeResponse;

public interface GetLearningOutcomeUseCase {
    GetLearningOutcomeResponse getLearningOutcome(UUID courseId, UUID learningOutcomeId);
    List<GetLearningOutcomeResponse> getLearningOutcomesByIds(UUID courseId, List<UUID> learningOutcomeIds);
}