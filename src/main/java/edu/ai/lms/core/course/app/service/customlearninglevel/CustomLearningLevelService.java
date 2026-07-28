package edu.ai.lms.core.course.app.service.customlearninglevel;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.*;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.CustomLearningLevel;
import edu.ai.lms.core.course.app.exception.CustomLearningLevelNotFoundException;
import edu.ai.lms.core.course.app.exception.CustomLearningLevelValidationException;
import edu.ai.lms.core.course.app.port.in.command.BatchEditCustomLearningLevelsCommand;
import edu.ai.lms.core.course.app.port.in.command.CustomLearningLevelValue;
import edu.ai.lms.core.course.app.port.in.usecase.CustomLearningLevelUseCase;
import edu.ai.lms.core.course.app.port.out.CustomLearningLevelPort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import edu.ai.lms.core.course.app.service.exammatrix.ExamMatrixAccess;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomLearningLevelService implements CustomLearningLevelUseCase {
    private final ExamMatrixAccess access;
    private final CustomLearningLevelPort levelPort;
    private final LoadExamMatrixPort matrixPort;

    @Override
    @Transactional
    public CustomLearningLevelResponse create(UUID courseId, UUID matrixId, CustomLearningLevelValue value) {
        Course course = access.requireCourseTeacher(courseId);
        requireMatrix(courseId, matrixId);
        requireOutcomesInCourse(course, value.learningOutcomeIds());
        CustomLearningLevel level = toDomain(value, UUID.randomUUID(), matrixId);
        List<CustomLearningLevel> finalLevels = new ArrayList<>(levelPort.lockAllByMatrixId(matrixId));
        finalLevels.add(level);
        validateUniqueFinalValues(finalLevels);
        levelPort.insert(level);
        return toResponse(levelPort.findByMatrixIdAndId(matrixId, level.getId())
                .orElseThrow(() -> notFound(level.getId())));
    }

    @Override
    public CustomLearningLevelListResponse getAll(UUID courseId, UUID matrixId) {
        access.requireCourseTeacher(courseId);
        requireMatrix(courseId, matrixId);
        return toListResponse(courseId, matrixId, levelPort.findAllByMatrixId(matrixId));
    }

    @Override
    public CustomLearningLevelResponse getOne(UUID courseId, UUID matrixId, UUID levelId) {
        access.requireCourseTeacher(courseId);
        requireMatrix(courseId, matrixId);
        return toResponse(levelPort.findByMatrixIdAndId(matrixId, levelId)
                .orElseThrow(() -> notFound(levelId)));
    }

    @Override
    @Transactional
    public BatchEditCustomLearningLevelsResponse batchEdit(BatchEditCustomLearningLevelsCommand command) {
        Course course = access.requireCourseTeacher(command.courseId());
        requireMatrix(command.courseId(), command.matrixId());
        List<CustomLearningLevelValue> creates = safe(command.create());
        List<CustomLearningLevelValue> updates = safe(command.update());
        List<UUID> deletes = safe(command.delete());
        if (creates.isEmpty() && updates.isEmpty() && deletes.isEmpty()) {
            throw invalid("Custom learning level batch cannot be empty");
        }

        requireUnique(creates.stream().map(CustomLearningLevelValue::clientTempId).toList(), "clientTempId");
        requireUnique(updates.stream().map(CustomLearningLevelValue::id).toList(), "update IDs");
        requireUnique(deletes, "delete IDs");

        Set<UUID> updateIds = updates.stream().map(CustomLearningLevelValue::id).collect(Collectors.toSet());
        Set<UUID> deleteIds = new HashSet<>(deletes);
        if (!Collections.disjoint(updateIds, deleteIds)) {
            throw invalid("A custom learning level cannot be updated and deleted in the same batch");
        }

        List<CustomLearningLevel> existing = levelPort.lockAllByMatrixId(command.matrixId());
        Map<UUID, CustomLearningLevel> existingById = existing.stream()
                .collect(Collectors.toMap(CustomLearningLevel::getId, Function.identity()));
        for (UUID id : updateIds) requireExisting(existingById, id);
        for (UUID id : deleteIds) requireExisting(existingById, id);
        creates.forEach(value -> requireOutcomesInCourse(course, value.learningOutcomeIds()));
        updates.forEach(value -> requireOutcomesInCourse(course, value.learningOutcomeIds()));
        List<CustomLearningLevel> updatedLevels = updates.stream()
                .map(value -> toDomain(value, value.id(), command.matrixId()))
                .toList();
        List<CustomLearningLevel> createdLevels = creates.stream()
                .map(value -> toDomain(value, UUID.randomUUID(), command.matrixId()))
                .toList();

        Map<UUID, CustomLearningLevel> updatesById = updatedLevels.stream()
                .collect(Collectors.toMap(CustomLearningLevel::getId, Function.identity()));
        List<CustomLearningLevel> finalLevels = existing.stream()
                .filter(level -> !deleteIds.contains(level.getId()))
                .map(level -> updatesById.getOrDefault(level.getId(), level))
                .collect(Collectors.toCollection(ArrayList::new));
        finalLevels.addAll(createdLevels);
        validateUniqueFinalValues(finalLevels);
        validateCompleteCoverage(course, finalLevels);

        deleteIds.forEach(levelPort::delete);
        updatedLevels.forEach(level ->
                levelPort.stageUpdate(level.getId(), "__tmp__" + level.getId()));
        updatedLevels.forEach(levelPort::update);
        createdLevels.forEach(levelPort::insert);

        List<CreatedCustomLearningLevelMappingResponse> mappings = new ArrayList<>();
        for (int index = 0; index < creates.size(); index++) {
            mappings.add(new CreatedCustomLearningLevelMappingResponse(
                    creates.get(index).clientTempId(), createdLevels.get(index).getId()));
        }
        return BatchEditCustomLearningLevelsResponse.builder()
                .createdLevelMappings(mappings)
                .result(toListResponse(command.courseId(), command.matrixId(),
                        levelPort.findAllByMatrixId(command.matrixId())))
                .build();
    }

    @Override
    @Transactional
    public DeleteCustomLearningLevelResponse delete(UUID courseId, UUID matrixId, UUID levelId) {
        Course course = access.requireCourseTeacher(courseId);
        requireMatrix(courseId, matrixId);
        levelPort.findByMatrixIdAndId(matrixId, levelId).orElseThrow(() -> notFound(levelId));
        List<CustomLearningLevel> finalLevels = levelPort.lockAllByMatrixId(matrixId).stream()
                .filter(level -> !levelId.equals(level.getId())).toList();
        validateCompleteCoverage(course, finalLevels);
        levelPort.delete(levelId);
        return new DeleteCustomLearningLevelResponse(
                courseId, matrixId, levelId, "Custom learning level deleted successfully", true);
    }

    private void requireOutcomesInCourse(Course course, List<UUID> learningOutcomeIds) {
        if (learningOutcomeIds == null || learningOutcomeIds.isEmpty()) {
            throw invalid("At least one learning outcome is required");
        }
        requireUnique(learningOutcomeIds, "learningOutcomeIds");
        Set<UUID> courseOutcomeIds = course.getLearningOutcomes().stream()
                .map(value -> value.getId()).collect(Collectors.toSet());
        if (!courseOutcomeIds.containsAll(learningOutcomeIds)) {
            throw invalid("One or more learning outcomes do not belong to the course");
        }
    }

    private CustomLearningLevel toDomain(CustomLearningLevelValue value, UUID id, UUID matrixId) {
        String code = normalizeRequired(value.code(), "Code");
        String name = normalizeRequired(value.name(), "Name");
        if (value.sortOrder() < 0) throw invalid("sortOrder must be >= 0");
        return CustomLearningLevel.builder()
                .id(id)
                .matrixId(matrixId)
                .learningOutcomeIds(value.learningOutcomeIds().stream().distinct().sorted().toList())
                .code(code)
                .name(name)
                .description(normalizeOptional(value.description()))
                .sortOrder(value.sortOrder())
                .build();
    }

    private void validateUniqueFinalValues(List<CustomLearningLevel> levels) {
        Set<String> codes = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (CustomLearningLevel level : levels) {
            String codeKey = level.getCode().toLowerCase(Locale.ROOT);
            String nameKey = level.getName();
            if (!codes.add(codeKey)) {
                throw invalid("Custom learning level code must be unique within the exam matrix");
            }
            if (!names.add(nameKey)) {
                throw invalid("Custom learning level name must be unique within the exam matrix");
            }
        }
    }

    private void requireExisting(Map<UUID, CustomLearningLevel> existing, UUID id) {
        if (id == null || !existing.containsKey(id)) throw notFound(id);
    }

    private void requireUnique(Collection<?> values, String field) {
        if (values.stream().anyMatch(value -> value == null
                || value instanceof String text && text.isBlank())) {
            throw invalid(field + " cannot contain blank or null values");
        }
        if (values.size() != new HashSet<>(values).size()) {
            throw invalid("Duplicate " + field + " are not allowed");
        }
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) throw invalid(field + " cannot be blank");
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateCompleteCoverage(Course course, List<CustomLearningLevel> levels) {
        if (levels.size() < 3) {
            throw invalid("An exam matrix must contain at least three custom learning levels");
        }
        if (levels.stream().anyMatch(level -> level.getLearningOutcomeIds().isEmpty())) {
            throw invalid("Every custom learning level must contain at least one learning outcome");
        }
        Set<UUID> covered = levels.stream().flatMap(level -> level.getLearningOutcomeIds().stream())
                .collect(Collectors.toSet());
        List<UUID> uncovered = course.getLearningOutcomes().stream().map(value -> value.getId())
                .filter(id -> !covered.contains(id)).toList();
        if (!uncovered.isEmpty()) {
            throw invalid("Every learning outcome must belong to at least one custom learning level; uncovered: "
                    + uncovered);
        }
    }

    private void requireMatrix(UUID courseId, UUID matrixId) {
        matrixPort.findByCourseIdAndId(courseId, matrixId)
                .orElseThrow(() -> new CustomLearningLevelNotFoundException(
                        "Exam matrix not found: " + matrixId));
    }

    private CustomLearningLevelListResponse toListResponse(
            UUID courseId, UUID matrixId, List<CustomLearningLevel> levels) {
        List<CustomLearningLevelResponse> responses = levels.stream().map(this::toResponse).toList();
        return CustomLearningLevelListResponse.builder()
                .courseId(courseId).matrixId(matrixId).total(responses.size()).levels(responses).build();
    }

    private CustomLearningLevelResponse toResponse(CustomLearningLevel level) {
        return CustomLearningLevelResponse.builder()
                .id(level.getId())
                .learningOutcomeIds(List.copyOf(level.getLearningOutcomeIds()))
                .code(level.getCode())
                .name(level.getName())
                .description(level.getDescription())
                .sortOrder(level.getSortOrder())
                .createdAt(level.getCreatedAt())
                .updatedAt(level.getUpdatedAt())
                .build();
    }

    private CustomLearningLevelNotFoundException notFound(UUID id) {
        return new CustomLearningLevelNotFoundException("Custom learning level not found: " + id);
    }

    private CustomLearningLevelValidationException invalid(String message) {
        return new CustomLearningLevelValidationException(message);
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
