package edu.ai.lms.core.course.app.exception;

import java.util.List;
import java.util.UUID;

public class ChapterCcctSyncConflictException extends RuntimeException {
    private final List<MatrixImpact> affectedMatrices;

    public ChapterCcctSyncConflictException(List<MatrixImpact> affectedMatrices) {
        super("Cannot delete this Chapter because its CCCT content is used by draft Matrix: "
                + affectedMatrices.stream().map(MatrixImpact::title).reduce((left, right) -> left + ", " + right).orElse(""));
        this.affectedMatrices = List.copyOf(affectedMatrices);
    }

    public List<MatrixImpact> getAffectedMatrices() { return affectedMatrices; }

    public record MatrixImpact(UUID id, String title, int affectedRowCount) { }
}
