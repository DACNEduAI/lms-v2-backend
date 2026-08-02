package edu.ai.lms.core.course.app.service.exammatrix;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.*;
import edu.ai.lms.core.course.app.exception.ExamMatrixValidationException;
import edu.ai.lms.core.course.app.port.in.command.ExamMatrixRowCommand;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixReferencePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ExamMatrixValidator {
    private final LoadExamMatrixReferencePort referencePort;

    List<ExamMatrixRow> validateAndMap(
            Course course,
            UUID matrixId,
            List<ExamMatrixRowCommand> commands) {
        requireUnique(commands.stream().map(ExamMatrixRowCommand::orderIndex).toList(), "orderIndex");
        Set<UUID> levelIds = commands.stream().flatMap(row -> row.levelCounts().stream())
                .map(value -> value.customLearningLevelId()).collect(Collectors.toSet());
        Map<UUID, CustomLearningLevel> levels = referencePort.findCustomLearningLevelsByIds(levelIds).stream()
                .collect(Collectors.toMap(CustomLearningLevel::getId, Function.identity()));
        if (levels.size() != levelIds.size()) {
            throw new ExamMatrixValidationException("One or more custom learning levels do not exist");
        }
        Map<UUID, Chapter> chapters = course.getChapters().stream()
                .collect(Collectors.toMap(Chapter::getChapterId, Function.identity()));
        Set<UUID> courseOutcomes = course.getLearningOutcomes().stream()
                .map(LearningOutcome::getId).collect(Collectors.toSet());
        List<ExamMatrixRow> result = new ArrayList<>();
        for (ExamMatrixRowCommand command : commands) {
            Chapter chapter = chapters.get(command.chapterId());
            if (chapter == null) {
                throw new ExamMatrixValidationException("Chapter does not belong to course: " + command.chapterId());
            }
            if (command.learningOutcomeIds().isEmpty()) {
                throw new ExamMatrixValidationException("Each row must contain at least one learning outcome");
            }
            requireUnique(command.learningOutcomeIds(), "learningOutcomeIds");
            if (!courseOutcomes.containsAll(command.learningOutcomeIds())) {
                throw new ExamMatrixValidationException("One or more learning outcomes do not belong to the course");
            }
            if (!new HashSet<>(chapter.getLearningOutcomeIds()).containsAll(command.learningOutcomeIds())) {
                throw new ExamMatrixValidationException("Every learning outcome must be assigned to the selected chapter");
            }
            String topic = command.topic() == null ? "" : command.topic().trim();
            if (topic.isEmpty()) {
                throw new ExamMatrixValidationException("Each row must contain a topic");
            }
            List<UUID> countIds = command.levelCounts().stream()
                    .map(value -> value.customLearningLevelId()).toList();
            requireUnique(countIds, "customLearningLevelIds");
            List<ExamMatrixLevelCount> counts = command.levelCounts().stream().map(value -> {
                if (value.questionType() != ExamMatrixQuestionType.MULTIPLE_CHOICE) {
                    throw new ExamMatrixValidationException(
                            "UNSUPPORTED_QUESTION_TYPE: Phase 4 supports MULTIPLE_CHOICE only");
                }
                CustomLearningLevel level = levels.get(value.customLearningLevelId());
                if (level == null || matrixId == null || !matrixId.equals(level.getMatrixId())) {
                    throw new ExamMatrixValidationException(
                            "Custom learning level does not belong to the exam matrix: "
                                    + value.customLearningLevelId());
                }
                List<UUID> mappedSelectedOutcomes = level.getLearningOutcomeIds().stream()
                        .filter(command.learningOutcomeIds()::contains).toList();
                return ExamMatrixLevelCount.builder()
                        .customLearningLevelId(level.getId())
                        .learningOutcomeIds(mappedSelectedOutcomes)
                        .code(level.getCode()).name(level.getName()).description(level.getDescription())
                        .sortOrder(level.getSortOrder()).questionType(value.questionType())
                        .questionCount(value.questionCount())
                        .active(!mappedSelectedOutcomes.isEmpty()).build();
            }).toList();
            result.add(ExamMatrixRow.builder()
                    .id(command.id()).matrixId(matrixId).chapterId(command.chapterId())
                    .topic(topic)
                    .orderIndex(command.orderIndex())
                    .learningOutcomeIds(new ArrayList<>(command.learningOutcomeIds()))
                    .levelCounts(new ArrayList<>(counts)).build());
        }
        return result;
    }

    void requireUniqueFinalOrder(Collection<Integer> indexes) {
        requireUnique(indexes, "final orderIndex");
    }

    boolean isPhase4Complete(ExamMatrix matrix) {
        return matrix.getRows().stream().allMatch(row ->
                        row.getChapterId() != null
                                && row.getTopic() != null
                                && !row.getTopic().isBlank()
                                && row.getLevelCounts().stream().allMatch(count ->
                                        !count.isActive()
                                                || count.getQuestionType()
                                                        == ExamMatrixQuestionType.MULTIPLE_CHOICE));
    }

    private void requireUnique(Collection<?> values, String field) {
        if (values.size() != new HashSet<>(values).size()) {
            throw new ExamMatrixValidationException("Duplicate " + field + " values are not allowed");
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new ExamMatrixValidationException(field + " cannot contain null values");
        }
    }
}
