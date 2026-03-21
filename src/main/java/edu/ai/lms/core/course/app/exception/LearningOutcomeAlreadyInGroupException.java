package edu.ai.lms.core.course.app.exception;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class LearningOutcomeAlreadyInGroupException extends RuntimeException {

    private final List<UUID> conflictingIds;

    public LearningOutcomeAlreadyInGroupException(String message, List<UUID> conflictingIds) {
        super(message);
        this.conflictingIds = conflictingIds;
    }
}
