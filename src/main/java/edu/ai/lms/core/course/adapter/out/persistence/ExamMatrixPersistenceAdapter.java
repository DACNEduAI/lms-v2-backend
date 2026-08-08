package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.*;
import edu.ai.lms.core.course.app.port.out.*;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ExamMatrixPersistenceAdapter implements SaveExamMatrixPort, LoadExamMatrixPort,
        DeleteExamMatrixPort, BatchEditExamMatrixPort, LoadExamMatrixReferencePort {

    private final ExamMatrixRepository repository;

    @Override
    public ExamMatrix save(ExamMatrix matrix) {
        matrix.setId(matrix.getId() == null ? UUID.randomUUID() : matrix.getId());
        repository.insertMatrix(matrix);
        if (matrix.getRows() != null) {
            matrix.getRows().forEach(row -> {
                row.setMatrixId(matrix.getId());
                insertRow(row);
            });
        }
        return loadFull(matrix.getId());
    }

    @Override
    public Optional<ExamMatrix> findById(UUID matrixId) {
        return repository.findById(matrixId).map(value -> loadFull(value.getId()));
    }

    @Override
    public Optional<ExamMatrix> findByCourseIdAndId(UUID courseId, UUID matrixId) {
        return repository.findByCourseIdAndId(courseId, matrixId).map(value -> loadFull(value.getId()));
    }

    @Override
    public List<ExamMatrixSummary> findAllSummariesByCourseId(UUID courseId) {
        return repository.findAllSummariesByCourseId(courseId);
    }

    @Override
    public Optional<ExamMatrix> lockByCourseIdAndId(UUID courseId, UUID matrixId) {
        return repository.lockByCourseIdAndId(courseId, matrixId).map(value -> loadFull(value.getId()));
    }

    @Override
    public void updateMetadata(UUID matrixId, String title, String description) {
        repository.updateMetadata(matrixId, title, description);
    }

    @Override
    public void deleteRow(UUID matrixId, UUID rowId) {
        repository.deleteRow(matrixId, rowId);
    }

    @Override
    public void replaceRow(ExamMatrixRow row) {
        repository.updateRow(row);
        repository.reconcileRowOutcomeCandidates(row.getId());
        insertChildren(row);
    }

    @Override
    public void insertRow(ExamMatrixRow row) {
        row.setId(row.getId() == null ? UUID.randomUUID() : row.getId());
        repository.insertRow(row);
        insertChildren(row);
    }

    @Override
    public void touch(UUID matrixId) {
        repository.touch(matrixId);
    }

    @Override
    public List<CustomLearningLevel> findCustomLearningLevelsByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<CustomLearningLevel> levels = repository.findCustomLearningLevelsByIds(ids);
        Map<UUID, CustomLearningLevel> byId = levels.stream()
                .collect(Collectors.toMap(CustomLearningLevel::getId, Function.identity()));
        repository.findCustomLearningLevelOutcomeLinksByIds(ids).stream()
                .filter(CustomLearningLevelOutcomeLink::selected).forEach(link ->
                        byId.get(link.customLearningLevelId()).getLearningOutcomeIds()
                                .add(link.learningOutcomeId()));
        return levels;
    }

    @Override
    public void deleteById(UUID matrixId) {
        repository.deleteById(matrixId);
    }

    private void insertChildren(ExamMatrixRow row) {
        repository.insertRowOutcomeCandidates(row.getId());
        repository.clearRowOutcomeSelections(row.getId());
        if (row.getLearningOutcomeIds() != null && !row.getLearningOutcomeIds().isEmpty()) {
            repository.selectRowOutcomes(row.getId(), row.getLearningOutcomeIds());
        }
        row.getLevelCounts().forEach(count -> repository.insertLevelCount(row.getId(), count));
    }

    private ExamMatrix loadFull(UUID matrixId) {
        ExamMatrix matrix = repository.findById(matrixId)
                .orElseThrow(() -> new IllegalStateException("Exam matrix disappeared: " + matrixId));
        List<ExamMatrixRow> rows = repository.findRowsByMatrixId(matrixId);
        Map<UUID, ExamMatrixRow> byId = rows.stream()
                .collect(Collectors.toMap(ExamMatrixRow::getId, Function.identity()));
        repository.findOutcomeLinksByMatrixId(matrixId).stream()
                .filter(ExamMatrixRowOutcomeLink::selected).forEach(link ->
                        byId.get(link.examMatrixRowId()).getLearningOutcomeIds().add(link.learningOutcomeId()));
        Map<String, ExamMatrixLevelCount> countsByKey = new java.util.HashMap<>();
        repository.findLevelCountsByMatrixId(matrixId).forEach(record -> {
            ExamMatrixLevelCount count = record.toDomain(new java.util.ArrayList<>());
            byId.get(record.examMatrixRowId()).getLevelCounts().add(count);
            countsByKey.put(countKey(record.examMatrixRowId(), record.customLearningLevelId()), count);
        });
        repository.findLevelCountOutcomeLinksByMatrixId(matrixId).forEach(link ->
                countsByKey.get(countKey(link.examMatrixRowId(), link.customLearningLevelId()))
                        .getLearningOutcomeIds().add(link.learningOutcomeId()));
        countsByKey.values().forEach(count -> count.setActive(!count.getLearningOutcomeIds().isEmpty()));
        List<CustomLearningLevel> levels = repository.findCustomLearningLevelsByMatrixId(matrixId);
        Map<UUID, CustomLearningLevel> levelsById = levels.stream()
                .collect(Collectors.toMap(CustomLearningLevel::getId, Function.identity()));
        if (!levelsById.isEmpty()) {
            repository.findCustomLearningLevelOutcomeLinksByIds(levelsById.keySet()).stream()
                    .filter(CustomLearningLevelOutcomeLink::selected)
                    .forEach(link -> levelsById.get(link.customLearningLevelId())
                            .getLearningOutcomeIds().add(link.learningOutcomeId()));
        }
        matrix.setCustomLearningLevels(levels);
        matrix.setUncoveredLearningOutcomeIds(repository.findUncoveredLearningOutcomeIds(matrixId));
        matrix.setRows(rows);
        return matrix;
    }

    private String countKey(UUID rowId, UUID levelId) {
        return rowId + ":" + levelId;
    }
}
