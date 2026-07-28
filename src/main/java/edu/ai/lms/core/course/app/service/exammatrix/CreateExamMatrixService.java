package edu.ai.lms.core.course.app.service.exammatrix;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixResponse;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.domain.CustomLearningLevel;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.in.command.CreateExamMatrixCommand;
import edu.ai.lms.core.course.app.port.in.command.ExamMatrixRowCommand;
import edu.ai.lms.core.course.app.port.in.command.LearningLevelCountCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateExamMatrixUseCase;
import edu.ai.lms.core.course.app.port.out.BatchEditExamMatrixPort;
import edu.ai.lms.core.course.app.port.out.CustomLearningLevelPort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import edu.ai.lms.core.course.app.port.out.SaveExamMatrixPort;
import edu.ai.lms.core.course.app.exception.ExamMatrixValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateExamMatrixService implements TeacherCreateExamMatrixUseCase {
    private final SaveExamMatrixPort savePort;
    private final ExamMatrixAccess access;
    private final ExamMatrixValidator validator;
    private final ExamMatrixResponseAssembler assembler;
    private final CustomLearningLevelPort levelPort;
    private final BatchEditExamMatrixPort batchPort;
    private final LoadExamMatrixPort loadPort;

    @Override
    @Transactional
    public ExamMatrixResponse createExamMatrix(CreateExamMatrixCommand command) {
        var course = access.requireCourseTeacher(command.courseId());
        UUID matrixId = UUID.randomUUID();
        List<CustomLearningLevel> levels = validateAndMapLevels(
                course.getLearningOutcomes(), matrixId, command.customLearningLevels());
        ExamMatrix matrix = ExamMatrix.builder()
                .id(matrixId)
                .courseId(command.courseId()).title(command.title())
                .description(command.description()).rows(List.of()).build();
        savePort.save(matrix);
        levels.forEach(levelPort::insert);

        Map<String, UUID> levelIdsByTempId = new HashMap<>();
        for (int index = 0; index < command.customLearningLevels().size(); index++) {
            levelIdsByTempId.put(
                    command.customLearningLevels().get(index).clientTempId(),
                    levels.get(index).getId());
        }
        List<ExamMatrixRowCommand> resolvedCommands = command.rows().stream()
                .map(row -> resolveLevelReferences(row, levelIdsByTempId)).toList();
        var rows = validator.validateAndMap(course, matrixId, resolvedCommands);
        rows.forEach(batchPort::insertRow);
        return assembler.toResponse(loadPort.findByCourseIdAndId(command.courseId(), matrixId)
                .orElseThrow(() -> new IllegalStateException("Exam matrix disappeared after creation")));
    }

    private List<CustomLearningLevel> validateAndMapLevels(
            List<LearningOutcome> courseOutcomes,
            UUID matrixId,
            List<edu.ai.lms.core.course.app.port.in.command.CustomLearningLevelValue> values) {
        if (values == null || values.size() < 3) {
            throw invalid("An exam matrix must contain at least three custom learning levels");
        }
        Set<UUID> courseOutcomeIds = courseOutcomes.stream().map(LearningOutcome::getId)
                .collect(Collectors.toSet());
        Set<String> tempIds = new HashSet<>();
        Set<String> codes = new HashSet<>();
        Set<String> names = new HashSet<>();
        Set<UUID> covered = new HashSet<>();
        List<CustomLearningLevel> levels = new ArrayList<>();
        for (var value : values) {
            if (value.clientTempId() == null || value.clientTempId().isBlank()
                    || !tempIds.add(value.clientTempId())) {
                throw invalid("Custom learning level clientTempId values must be non-blank and unique");
            }
            String code = normalizeRequired(value.code(), "Custom learning level code");
            String name = normalizeRequired(value.name(), "Custom learning level name");
            if (!codes.add(code.toLowerCase(Locale.ROOT)) || !names.add(name)) {
                throw invalid("Custom learning level codes and names must be unique within the exam matrix");
            }
            List<UUID> outcomeIds = value.learningOutcomeIds() == null
                    ? List.of() : value.learningOutcomeIds().stream().distinct().sorted().toList();
            if (outcomeIds.isEmpty() || !courseOutcomeIds.containsAll(outcomeIds)) {
                throw invalid("Every custom learning level must select valid course learning outcomes");
            }
            covered.addAll(outcomeIds);
            levels.add(CustomLearningLevel.builder()
                    .id(UUID.randomUUID()).matrixId(matrixId).learningOutcomeIds(outcomeIds)
                    .code(code).name(name)
                    .description(value.description() == null || value.description().isBlank()
                            ? null : value.description().trim())
                    .sortOrder(value.sortOrder()).build());
        }
        if (!covered.containsAll(courseOutcomeIds)) {
            throw invalid("Every course learning outcome must belong to at least one custom learning level");
        }
        return levels;
    }

    private ExamMatrixRowCommand resolveLevelReferences(
            ExamMatrixRowCommand row,
            Map<String, UUID> levelIdsByTempId) {
        var counts = row.levelCounts().stream().map(count -> {
            UUID id = count.customLearningLevelId();
            if (id == null && count.customLearningLevelClientTempId() != null) {
                id = levelIdsByTempId.get(count.customLearningLevelClientTempId());
            }
            if (id == null) {
                throw invalid("Each question count must reference a custom learning level in the request");
            }
            return LearningLevelCountCommand.builder()
                    .customLearningLevelId(id).questionCount(count.questionCount()).build();
        }).toList();
        return ExamMatrixRowCommand.builder()
                .id(row.id()).clientTempId(row.clientTempId()).chapterId(row.chapterId())
                .topic(row.topic()).orderIndex(row.orderIndex())
                .learningOutcomeIds(row.learningOutcomeIds()).levelCounts(counts).build();
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) throw invalid(field + " cannot be blank");
        return value.trim();
    }

    private ExamMatrixValidationException invalid(String message) {
        return new ExamMatrixValidationException(message);
    }
}
