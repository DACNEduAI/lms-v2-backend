package edu.ai.lms.core.course.app.service.exammatrix;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.*;
import edu.ai.lms.core.course.app.exception.ExamMatrixValidationException;
import edu.ai.lms.core.course.app.port.in.command.ExamMatrixRowCommand;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixReferencePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ExamMatrixValidator {
    private final LoadExamMatrixReferencePort referencePort;
    private final LoadCcctProfilePort ccctProfilePort;

    CcctProfile requirePublishedProfile(UUID courseId, UUID profileId) {
        if (profileId == null) {
            throw new ExamMatrixValidationException("CCCT profile ID is required");
        }
        CcctProfile profile = ccctProfilePort.findByCourseIdAndId(courseId, profileId)
                .orElseThrow(() -> new ExamMatrixValidationException(
                        "CCCT profile does not belong to the course: " + profileId));
        if (profile.getStatus() != CcctProfileStatus.PUBLISHED) {
            throw new ExamMatrixValidationException(
                    "A new exam matrix must use the course's published CCCT profile");
        }
        return profile;
    }

    CcctProfile requirePinnedProfile(
            UUID courseId,
            UUID requestedProfileId,
            UUID existingProfileId) {
        if (existingProfileId == null) {
            return requirePublishedProfile(courseId, requestedProfileId);
        }
        if (!existingProfileId.equals(requestedProfileId)) {
            throw new ExamMatrixValidationException(
                    "An exam matrix cannot be retargeted to a different CCCT profile");
        }
        CcctProfile profile = ccctProfilePort.findByCourseIdAndId(courseId, existingProfileId)
                .orElseThrow(() -> new ExamMatrixValidationException(
                        "The pinned CCCT profile no longer exists"));
        if (profile.getStatus() != CcctProfileStatus.PUBLISHED
                && profile.getStatus() != CcctProfileStatus.ARCHIVED) {
            throw new ExamMatrixValidationException(
                    "The pinned CCCT profile must be published or archived");
        }
        return profile;
    }

    List<ExamMatrixRow> validateAndMap(
            Course course,
            UUID matrixId,
            CcctProfile ccctProfile,
            List<ExamMatrixRowCommand> commands) {
        requireUnique(commands.stream().map(ExamMatrixRowCommand::orderIndex).toList(), "orderIndex");
        requireUnique(commands.stream().map(ExamMatrixRowCommand::ccctItemId).toList(), "ccctItemId");
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
        Map<UUID, CcctItemBinding> eligibleItems = eligibleItems(ccctProfile);

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
            CcctItemBinding item = eligibleItems.get(command.ccctItemId());
            if (item == null) {
                throw new ExamMatrixValidationException(
                        "CCCT item is not eligible for an exam matrix: " + command.ccctItemId());
            }
            if (!command.chapterId().equals(item.courseChapterId())) {
                throw new ExamMatrixValidationException(
                        "CCCT item does not belong to the selected course chapter");
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
                    .ccctItemId(command.ccctItemId()).topic(item.content())
                    .orderIndex(command.orderIndex())
                    .learningOutcomeIds(new ArrayList<>(command.learningOutcomeIds()))
                    .levelCounts(new ArrayList<>(counts)).build());
        }
        return result;
    }

    void requireUniqueFinalOrder(Collection<Integer> indexes) {
        requireUnique(indexes, "final orderIndex");
    }

    void requireUniqueFinalCcctItems(Collection<UUID> itemIds) {
        requireUnique(itemIds, "final ccctItemId");
    }

    boolean isPhase4Complete(ExamMatrix matrix) {
        return matrix.getCcctProfileId() != null
                && matrix.getRows().stream().allMatch(row ->
                        row.getChapterId() != null
                                && row.getCcctItemId() != null
                                && row.getLevelCounts().stream().allMatch(count ->
                                        count.getQuestionType() == ExamMatrixQuestionType.MULTIPLE_CHOICE));
    }

    private Map<UUID, CcctItemBinding> eligibleItems(CcctProfile profile) {
        Map<UUID, CcctItemBinding> result = new HashMap<>();
        for (CcctChapter chapter : profile.getChapters()) {
            if (chapter.getCourseChapterId() == null) {
                continue;
            }
            for (CcctTopic topic : chapter.getTopics()) {
                for (CcctItem item : topic.getItems()) {
                    if (item.getCoverageStatus() == CcctCoverageStatus.LEARN_AND_EXAM) {
                        result.put(item.getId(), new CcctItemBinding(
                                chapter.getCourseChapterId(), item.getContent()));
                    }
                }
            }
        }
        return result;
    }

    private void requireUnique(Collection<?> values, String field) {
        if (values.size() != new HashSet<>(values).size()) {
            throw new ExamMatrixValidationException("Duplicate " + field + " values are not allowed");
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new ExamMatrixValidationException(field + " cannot contain null values");
        }
    }

    private record CcctItemBinding(UUID courseChapterId, String content) {
    }
}
