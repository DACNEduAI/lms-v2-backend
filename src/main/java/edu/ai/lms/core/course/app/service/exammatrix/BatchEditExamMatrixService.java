package edu.ai.lms.core.course.app.service.exammatrix;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.BatchEditExamMatrixResponse;
import edu.ai.lms.core.course.adapter.in.response.CreatedRowMappingResponse;
import edu.ai.lms.core.course.app.exception.ExamMatrixNotFoundException;
import edu.ai.lms.core.course.app.exception.ExamMatrixValidationException;
import edu.ai.lms.core.course.app.port.in.command.BatchEditExamMatrixCommand;
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

    @Override
    @Transactional
    public BatchEditExamMatrixResponse batchEdit(BatchEditExamMatrixCommand command) {
        var course = access.requireCourseTeacher(command.courseId());
        if (command.metadata() == null && command.create().isEmpty()
                && command.update().isEmpty() && command.delete().isEmpty()) {
            throw new ExamMatrixValidationException("Batch must contain at least one change");
        }
        var existing = batchPort.lockByCourseIdAndId(command.courseId(), command.matrixId())
                .orElseThrow(() -> new ExamMatrixNotFoundException("Exam matrix not found: " + command.matrixId()));

        requireUnique(command.create().stream().map(value -> value.clientTempId()).toList(), "clientTempId");
        Set<UUID> updateIds = requireUnique(command.update().stream().map(value -> value.id()).toList(), "update row ID");
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

        var updatedRows = validator.validateAndMap(course, command.matrixId(), command.update());
        var createdRows = validator.validateAndMap(course, command.matrixId(), command.create());
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
        return BatchEditExamMatrixResponse.builder()
                .createdRowMappings(mappings).matrix(assembler.toResponse(reloaded)).build();
    }

    private <T> Set<T> requireUnique(Collection<T> values, String label) {
        if (values.stream().anyMatch(Objects::isNull) || new HashSet<>(values).size() != values.size()) {
            throw new ExamMatrixValidationException("Duplicate or null " + label + " values are not allowed");
        }
        return new HashSet<>(values);
    }
}
