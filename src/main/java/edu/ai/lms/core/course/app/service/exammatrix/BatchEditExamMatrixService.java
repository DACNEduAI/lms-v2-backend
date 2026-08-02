package edu.ai.lms.core.course.app.service.exammatrix;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.BatchEditExamMatrixResponse;
import edu.ai.lms.core.course.adapter.in.response.CreatedRowMappingResponse;
import edu.ai.lms.core.course.adapter.in.response.CreatedCustomLearningLevelMappingResponse;
import edu.ai.lms.core.course.app.exception.ExamMatrixNotFoundException;
import edu.ai.lms.core.course.app.exception.ExamMatrixValidationException;
import edu.ai.lms.core.course.app.port.in.command.BatchEditExamMatrixCommand;
import edu.ai.lms.core.course.app.port.in.command.ExamMatrixRowCommand;
import edu.ai.lms.core.course.app.port.in.command.LearningLevelCountCommand;
import edu.ai.lms.core.course.app.port.in.usecase.CustomLearningLevelUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherBatchEditExamMatrixUseCase;
import edu.ai.lms.core.course.app.port.out.BatchEditExamMatrixPort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchEditExamMatrixService implements TeacherBatchEditExamMatrixUseCase {
    private final BatchEditExamMatrixPort batchPort;
    private final LoadExamMatrixPort loadPort;
    private final ExamMatrixAccess access;
    private final ExamMatrixValidator validator;
    private final ExamMatrixResponseAssembler assembler;
    private final CustomLearningLevelUseCase customLearningLevelUseCase;

    @Override
    @Transactional
    public BatchEditExamMatrixResponse batchEdit(BatchEditExamMatrixCommand command) {
        var course = access.requireCourseTeacher(command.courseId());
        if (command.metadata() == null && command.customLearningLevels() == null && command.create().isEmpty()
                && command.update().isEmpty() && command.delete().isEmpty()) {
            throw new ExamMatrixValidationException("Batch must contain at least one change");
        }
        var existing = batchPort.lockByCourseIdAndId(command.courseId(), command.matrixId())
                .orElseThrow(() -> new ExamMatrixNotFoundException("Exam matrix not found: " + command.matrixId()));
        List<CreatedCustomLearningLevelMappingResponse> createdLevelMappings = List.of();
        Map<String, UUID> resolvedCreatedLevelIds = Map.of();
        if (command.customLearningLevels() != null) {
            var levelResponse = customLearningLevelUseCase.batchEdit(command.customLearningLevels());
            createdLevelMappings = levelResponse.createdLevelMappings();
            resolvedCreatedLevelIds = createdLevelMappings.stream().collect(Collectors.toMap(
                    CreatedCustomLearningLevelMappingResponse::clientTempId,
                    CreatedCustomLearningLevelMappingResponse::customLearningLevelId));
        }
        final Map<String, UUID> createdLevelIds = resolvedCreatedLevelIds;

        List<ExamMatrixRowCommand> createCommands = command.create().stream()
                .map(row -> resolveLevelReferences(row, createdLevelIds)).toList();
        List<ExamMatrixRowCommand> updateCommands = command.update().stream()
                .map(row -> resolveLevelReferences(row, createdLevelIds)).toList();

        requireUnique(createCommands.stream().map(value -> value.clientTempId()).toList(), "clientTempId");
        Set<UUID> updateIds = requireUnique(updateCommands.stream().map(value -> value.id()).toList(), "update row ID");
        Set<UUID> deleteIds = requireUnique(command.delete(), "delete row ID");
        if (!Collections.disjoint(updateIds, deleteIds)) {
            throw new ExamMatrixValidationException("A row cannot be both updated and deleted");
        }
        Set<UUID> currentIds = existing.getRows().stream().map(value -> value.getId()).collect(Collectors.toSet());
        Set<UUID> targeted = new HashSet<>(updateIds);
        targeted.addAll(deleteIds);
        if (!currentIds.containsAll(targeted)) {
            throw new ExamMatrixNotFoundException("One or more rows do not belong to the exam matrix");
        }

        var updatedRows = validator.validateAndMap(course, command.matrixId(), updateCommands);
        var createdRows = validator.validateAndMap(course, command.matrixId(), createCommands);
        createdRows.forEach(row -> row.setId(UUID.randomUUID()));

        List<Integer> finalOrder = existing.getRows().stream()
                .filter(row -> !targeted.contains(row.getId()))
                .map(row -> row.getOrderIndex()).collect(Collectors.toCollection(ArrayList::new));
        updatedRows.forEach(row -> finalOrder.add(row.getOrderIndex()));
        createdRows.forEach(row -> finalOrder.add(row.getOrderIndex()));
        validator.requireUniqueFinalOrder(finalOrder);
        command.delete().forEach(id -> batchPort.deleteRow(command.matrixId(), id));
        updatedRows.forEach(batchPort::replaceRow);
        createdRows.forEach(batchPort::insertRow);
        if (command.metadata() != null) {
            batchPort.updateMetadata(command.matrixId(), command.metadata().title(), command.metadata().description());
        } else {
            batchPort.touch(command.matrixId());
        }

        var mappings = new ArrayList<CreatedRowMappingResponse>();
        for (int i = 0; i < command.create().size(); i++) {
            mappings.add(CreatedRowMappingResponse.builder()
                    .clientTempId(command.create().get(i).clientTempId())
                    .rowId(createdRows.get(i).getId()).build());
        }
        var reloaded = loadPort.findByCourseIdAndId(command.courseId(), command.matrixId())
                .orElseThrow(() -> new IllegalStateException("Exam matrix disappeared after batch edit"));
        boolean incomplete = reloaded.getCustomLearningLevels().size() < 3
                || !reloaded.getUncoveredLearningOutcomeIds().isEmpty()
                || reloaded.getCustomLearningLevels().stream()
                        .anyMatch(level -> level.getLearningOutcomeIds().isEmpty())
                || reloaded.getRows().stream().anyMatch(row -> row.getLearningOutcomeIds().isEmpty())
                || !validator.isPhase4Complete(reloaded);
        if (incomplete) {
            throw new ExamMatrixValidationException(
                    "The submitted configuration is incomplete; every outcome must be covered and every row/level valid");
        }
        return BatchEditExamMatrixResponse.builder()
                .createdRowMappings(mappings).createdLevelMappings(createdLevelMappings)
                .matrix(assembler.toResponse(reloaded)).build();
    }

    private ExamMatrixRowCommand resolveLevelReferences(
            ExamMatrixRowCommand row,
            Map<String, UUID> createdLevelIds) {
        var counts = row.levelCounts().stream().map(count -> {
            UUID id = count.customLearningLevelId();
            if (id == null && count.customLearningLevelClientTempId() != null) {
                id = createdLevelIds.get(count.customLearningLevelClientTempId());
            }
            if (id == null) {
                throw new ExamMatrixValidationException(
                        "Question count references an unknown custom learning level");
            }
            return LearningLevelCountCommand.builder()
                    .customLearningLevelId(id).questionType(count.questionType())
                    .questionCount(count.questionCount()).build();
        }).toList();
        return ExamMatrixRowCommand.builder()
                .id(row.id()).clientTempId(row.clientTempId()).chapterId(row.chapterId())
                .topic(row.topic()).orderIndex(row.orderIndex())
                .learningOutcomeIds(row.learningOutcomeIds()).levelCounts(counts).build();
    }

    private <T> Set<T> requireUnique(Collection<T> values, String label) {
        if (values.stream().anyMatch(Objects::isNull) || new HashSet<>(values).size() != values.size()) {
            throw new ExamMatrixValidationException("Duplicate or null " + label + " values are not allowed");
        }
        return new HashSet<>(values);
    }
}
