package edu.ai.lms.core.quiz.app.domain.assessment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class AssessmentModels {
    private AssessmentModels() {
    }

    public enum LifecycleStatus {
        DRAFT, GENERATING, REVIEW_PENDING, READY_TO_PUBLISH, PUBLISHED, ARCHIVED
    }

    public enum SourceLifecycleStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }

    public record AssessmentView(
            UUID id,
            UUID courseId,
            String title,
            String description,
            boolean unlimited,
            Integer timeLimitMinutes,
            Double passScore,
            LocalDateTime startTime,
            LocalDateTime dueTime,
            Integer attemptLimited,
            int variantCount,
            int optionCount,
            boolean allowRecentlyUsedQuestions,
            LifecycleStatus lifecycleStatus,
            int currentStep,
            UUID ccctProfileId,
            Long ccctProfileVersion,
            UUID examMatrixId,
            Long examMatrixVersion,
            boolean aiGenerationStarted,
            long version,
            UUID createdBy,
            UUID publishedBy,
            LocalDateTime publishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record SourceOption(
            UUID id,
            String title,
            SourceLifecycleStatus status,
            long version,
            boolean editable,
            LocalDateTime updatedAt) {
    }

    public record ContextView(
            AssessmentView assessment,
            List<SourceOption> ccctProfiles,
            List<SourceOption> examMatrices) {
    }

    public record BankPlan(
            UUID assessmentId,
            UUID examMatrixId,
            boolean allowRecentlyUsedQuestions,
            int required,
            int validated,
            int recentlyUsed,
            int reserved,
            int available,
            int missing,
            List<BankPlanCell> cells) {
    }

    public record BankPlanCell(
            UUID matrixRowId,
            UUID customLearningLevelId,
            UUID chapterId,
            String chapter,
            String topic,
            String levelCode,
            String levelName,
            List<UUID> learningOutcomeIds,
            int required,
            int validated,
            int recentlyUsed,
            int reserved,
            int available,
            int missing) {
    }

    public record BatchView(
            UUID id,
            UUID assessmentId,
            String status,
            int optionCount,
            int totalCells,
            int completedCells,
            int generatedQuestions,
            String errorMessage,
            LocalDateTime createdAt,
            LocalDateTime completedAt,
            List<BatchCellView> cells) {
    }

    public record BatchCellView(
            UUID id,
            UUID matrixRowId,
            UUID customLearningLevelId,
            UUID chapterId,
            String chapter,
            String topic,
            String levelCode,
            String levelName,
            List<UUID> learningOutcomeIds,
            int requiredCount,
            int generatedCount,
            String status,
            String errorMessage,
            int retryCount) {
    }

    public record GeneratedDraft(
            String content,
            String type,
            UUID matrixRowId,
            UUID customLearningLevelId,
            List<UUID> learningOutcomeIds,
            List<GeneratedAnswer> answers) {
    }

    public record GeneratedAnswer(String content, boolean correct, String explanation) {
    }

    public record DraftReviewItem(
            UUID id,
            UUID bankQuestionId,
            UUID generationBatchId,
            UUID matrixRowId,
            UUID customLearningLevelId,
            String content,
            String questionType,
            String cognitiveLevelCode,
            UUID chapterId,
            List<UUID> learningOutcomeIds,
            List<AnswerView> answers,
            String status,
            boolean duplicateWarning,
            boolean recentlyUsed,
            long version,
            UUID reviewedBy,
            LocalDateTime reviewedAt) {
    }

    public record AnswerView(UUID id, String content, boolean correct, String explanation, int orderIndex) {
    }

    public record DraftReviewView(
            UUID assessmentId,
            List<DraftReviewItem> notValidated,
            List<DraftReviewItem> validated,
            List<DraftReviewItem> rejected) {
    }

    public record VariantView(
            UUID id,
            UUID quizId,
            String code,
            int orderIndex,
            long shuffleSeed,
            String coverageStatus,
            String overrideReason,
            UUID overrideBy,
            LocalDateTime overrideAt,
            List<VariantQuestionView> questions) {
    }

    public record VariantQuestionView(
            UUID id,
            UUID sourceBankQuestionId,
            UUID chapterId,
            UUID cognitiveLevelId,
            String content,
            String type,
            List<UUID> learningOutcomeIds,
            List<AnswerView> answers,
            int orderIndex) {
    }

    public record PublishResult(
            UUID assessmentId,
            LifecycleStatus status,
            int publishedVariants,
            int recentlyUsedQuestions,
            LocalDateTime publishedAt) {
    }
}
