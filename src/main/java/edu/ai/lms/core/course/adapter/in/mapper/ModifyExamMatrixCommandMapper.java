package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.ModifyExamMatrixRequest;
import edu.ai.lms.core.course.app.port.in.command.ExamMatrixRowCommand;
import edu.ai.lms.core.course.app.port.in.command.ModifyExamMatrixCommand;

/**
 * Maps a {@link ModifyExamMatrixRequest} to a {@link ModifyExamMatrixCommand}.
 */
@Component
public class ModifyExamMatrixCommandMapper {

    public ModifyExamMatrixCommand toCommand(UUID courseId, UUID matrixId, ModifyExamMatrixRequest request) {
        List<ExamMatrixRowCommand> rowCommands = request.getRows().stream()
                .map(r -> ExamMatrixRowCommand.builder()
                        .id(r.getId())
                        .learningOutcomeId(r.getLearningOutcomeId())
                        .chapterId(r.getChapterId())
                        .topic(r.getTopic())
                        .understandCount(r.getUnderstandCount())
                        .applyCount(r.getApplyCount())
                        .applyPlusCount(r.getApplyPlusCount())
                        .orderIndex(r.getOrderIndex())
                        .build())
                .collect(Collectors.toList());

        return ModifyExamMatrixCommand.builder()
                .matrixId(matrixId)
                .courseId(courseId)
                .title(request.getTitle())
                .description(request.getDescription())
                .rows(rowCommands)
                .build();
    }
}
