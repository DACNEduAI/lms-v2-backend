package edu.ai.lms.core.course.adapter.in.response;

import java.util.UUID;

public record DeleteCustomLearningLevelResponse(
        UUID courseId,
        UUID customLearningLevelId,
        String message,
        boolean success) {
}
