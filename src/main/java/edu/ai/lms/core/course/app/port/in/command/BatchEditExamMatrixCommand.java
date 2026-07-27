package edu.ai.lms.core.course.app.port.in.command;

import java.util.List;
import java.util.UUID;

import lombok.Builder;

@Builder
public record BatchEditExamMatrixCommand(
        UUID courseId,
        UUID matrixId,
        MatrixMetadataCommand metadata,
        List<ExamMatrixRowCommand> create,
        List<ExamMatrixRowCommand> update,
        List<UUID> delete
) {
    public BatchEditExamMatrixCommand {
        create = create == null ? List.of() : List.copyOf(create);
        update = update == null ? List.of() : List.copyOf(update);
        delete = delete == null ? List.of() : List.copyOf(delete);
    }
}
