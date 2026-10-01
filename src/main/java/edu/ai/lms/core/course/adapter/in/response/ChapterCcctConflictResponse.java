package edu.ai.lms.core.course.adapter.in.response;

import java.util.List;
import java.util.UUID;

public record ChapterCcctConflictResponse(
        int status,
        String error,
        String message,
        List<AffectedMatrix> affectedMatrices) {
    public record AffectedMatrix(UUID id, String title, int affectedRowCount) { }
}
