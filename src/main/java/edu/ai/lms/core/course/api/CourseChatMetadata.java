package edu.ai.lms.core.course.api;

import java.util.List;
import java.util.UUID;

/** Trusted course metadata that may be supplied to the course tutor. */
public record CourseChatMetadata(
        UUID id,
        String code,
        String name,
        String description,
        List<LearningOutcomeView> learningOutcomes
) {
    public CourseChatMetadata {
        learningOutcomes = learningOutcomes == null ? List.of() : List.copyOf(learningOutcomes);
    }
}
