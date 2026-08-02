package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.BatchEditExamMatrixRequest;
import edu.ai.lms.core.course.app.port.in.command.*;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BatchEditExamMatrixCommandMapper {
    private final CreateExamMatrixCommandMapper rowMapper;
    private final CustomLearningLevelCommandMapper customLearningLevelMapper;

    public BatchEditExamMatrixCommand toCommand(UUID courseId, UUID matrixId, BatchEditExamMatrixRequest request) {
        MatrixMetadataCommand metadata = request.getMetadata() == null ? null
                : new MatrixMetadataCommand(request.getMetadata().getTitle(), request.getMetadata().getDescription());
        return BatchEditExamMatrixCommand.builder()
                .courseId(courseId).matrixId(matrixId)
                .metadata(metadata)
                .customLearningLevels(request.getCustomLearningLevels() == null ? null
                        : customLearningLevelMapper.toCommand(
                                courseId, matrixId, request.getCustomLearningLevels()))
                .create(request.getCreate().stream().map(value -> {
                    var row = rowMapper.toRow(value);
                    return ExamMatrixRowCommand.builder()
                            .clientTempId(value.getClientTempId()).chapterId(row.chapterId())
                            .topic(row.topic()).orderIndex(row.orderIndex())
                            .learningOutcomeIds(row.learningOutcomeIds()).levelCounts(row.levelCounts()).build();
                }).toList())
                .update(request.getUpdate().stream().map(value -> {
                    var row = rowMapper.toRow(value);
                    return ExamMatrixRowCommand.builder()
                            .id(value.getId()).chapterId(row.chapterId()).topic(row.topic())
                            .orderIndex(row.orderIndex()).learningOutcomeIds(row.learningOutcomeIds())
                            .levelCounts(row.levelCounts()).build();
                }).toList())
                .delete(request.getDelete()).build();
    }
}
