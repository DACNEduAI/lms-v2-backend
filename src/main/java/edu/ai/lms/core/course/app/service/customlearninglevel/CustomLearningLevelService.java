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
import edu.ai.lms.core.course.app.service.exammatrix.ExamMatrixAccess;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomLearningLevelService implements CustomLearningLevelUseCase {
    private final ExamMatrixAccess access;
    private final CustomLearningLevelPort levelPort;

    @Override
    @Transactional
    public CustomLearningLevelResponse create(UUID courseId, CustomLearningLevelValue value) {
        Course course = access.requireCourseTeacher(courseId);
        requireOutcomesInCourse(course, value.learningOutcomeIds());
        CustomLearningLevel level = toDomain(value, UUID.randomUUID(), courseId);
        List<CustomLearningLevel> finalLevels = new ArrayList<>(levelPort.lockAllByCourseId(courseId));
        finalLevels.add(level);
        validateUniqueFinalValues(finalLevels);
        levelPort.insert(level);
        return toResponse(levelPort.findByCourseIdAndId(courseId, level.getId())
                .orElseThrow(() -> notFound(level.getId())));
    }

    @Override
    public CustomLearningLevelListResponse getAll(UUID courseId) {
        access.requireCourseTeacher(courseId);
        return toListResponse(courseId, levelPort.findAllByCourseId(courseId));
    }

    @Override
    public CustomLearningLevelResponse getOne(UUID courseId, UUID levelId) {
        access.requireCourseTeacher(courseId);
        return toResponse(levelPort.findByCourseIdAndId(courseId, levelId)
                .orElseThrow(() -> notFound(levelId)));
    }

    @Override
    @Transactional
    public BatchEditCustomLearningLevelsResponse batchEdit(BatchEditCustomLearningLevelsCommand command) {
        Course course = access.requireCourseTeacher(command.courseId());
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

        List<CustomLearningLevel> existing = levelPort.lockAllByCourseId(command.courseId());
        Map<UUID, CustomLearningLevel> existingById = existing.stream()
                .collect(Collectors.toMap(CustomLearningLevel::getId, Function.identity()));
        for (UUID id : updateIds) requireExisting(existingById, id);
        for (UUID id : deleteIds) requireExisting(existingById, id);
        creates.forEach(value -> requireOutcomesInCourse(course, value.learningOutcomeIds()));
        updates.forEach(value -> requireOutcomesInCourse(course, value.learningOutcomeIds()));
        rejectUsedDeletes(deleteIds);

        List<CustomLearningLevel> updatedLevels = updates.stream()
                .map(value -> toDomain(value, value.id(), command.courseId()))
                .toList();
        List<CustomLearningLevel> createdLevels = creates.stream()
                .map(value -> toDomain(value, UUID.randomUUID(), command.courseId()))
                .toList();
        updatedLevels.forEach(level -> {
            long incompatible = levelPort.countIncompatibleMatrixUsages(
                    level.getId(), level.getLearningOutcomeIds());
            if (incompatible > 0) {
                throw invalid("Removing these learning outcome mappings would invalidate "
                        + incompatible + " exam matrix count record(s) for custom learning level "
                        + level.getId());
            }
        });

        Map<UUID, CustomLearningLevel> updatesById = updatedLevels.stream()
                .collect(Collectors.toMap(CustomLearningLevel::getId, Function.identity()));
        List<CustomLearningLevel> finalLevels = existing.stream()
                .filter(level -> !deleteIds.contains(level.getId()))
                .map(level -> updatesById.getOrDefault(level.getId(), level))
                .collect(Collectors.toCollection(ArrayList::new));
        finalLevels.addAll(createdLevels);
        validateUniqueFinalValues(finalLevels);

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
                .result(toListResponse(command.courseId(), levelPort.findAllByCourseId(command.courseId())))
                .build();
    }

    @Override
    @Transactional
    public DeleteCustomLearningLevelResponse delete(UUID courseId, UUID levelId) {
        access.requireCourseTeacher(courseId);
        levelPort.findByCourseIdAndId(courseId, levelId).orElseThrow(() -> notFound(levelId));
        rejectUsedDeletes(Set.of(levelId));
        levelPort.delete(levelId);
        return new DeleteCustomLearningLevelResponse(
                courseId, levelId, "Custom learning level deleted successfully", true);
    }

    private void rejectUsedDeletes(Set<UUID> ids) {
        for (UUID id : ids) {
            long usages = levelPort.countMatrixUsages(id);
            if (usages > 0) {
                throw invalid("Custom learning level " + id + " is used by " + usages
                        + " exam matrix count record(s)");
            }
        }
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

    private CustomLearningLevel toDomain(CustomLearningLevelValue value, UUID id, UUID courseId) {
        String code = normalizeRequired(value.code(), "Code");
        String name = normalizeRequired(value.name(), "Name");
        if (value.sortOrder() < 0) throw invalid("sortOrder must be >= 0");
        return CustomLearningLevel.builder()
                .id(id)
                .courseId(courseId)
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
                throw invalid("Custom learning level code must be unique within the course");
            }
            if (!names.add(nameKey)) {
                throw invalid("Custom learning level name must be unique within the course");
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

    private CustomLearningLevelListResponse toListResponse(UUID courseId, List<CustomLearningLevel> levels) {
        List<CustomLearningLevelResponse> responses = levels.stream().map(this::toResponse).toList();
        return CustomLearningLevelListResponse.builder()
                .courseId(courseId).total(responses.size()).levels(responses).build();
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
