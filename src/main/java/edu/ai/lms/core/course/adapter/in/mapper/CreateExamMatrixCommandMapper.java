package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.CreateExamMatrixRequest;
import edu.ai.lms.core.course.app.port.in.command.CreateExamMatrixCommand;
import edu.ai.lms.core.course.app.port.in.command.ExamMatrixRowCommand;

/**
 * Maps a {@link CreateExamMatrixRequest} to a {@link CreateExamMatrixCommand}.
 */
@Component
public class CreateExamMatrixCommandMapper {

    public CreateExamMatrixCommand toCommand(UUID courseId, CreateExamMatrixRequest request) {
        List<ExamMatrixRowCommand> rowCommands = request.getRows().stream()
                .map(r -> ExamMatrixRowCommand.builder()
                        .learningOutcomeId(r.getLearningOutcomeId())
                        .chapterId(r.getChapterId())
                        .topic(r.getTopic())
                        .understandCount(r.getUnderstandCount())
                        .applyCount(r.getApplyCount())
                        .applyPlusCount(r.getApplyPlusCount())
                        .orderIndex(r.getOrderIndex())
                        .build())
                .collect(Collectors.toList());

        return CreateExamMatrixCommand.builder()
                .courseId(courseId)
                .title(request.getTitle())
                .description(request.getDescription())
                .rows(rowCommands)
                .build();
    }
}
