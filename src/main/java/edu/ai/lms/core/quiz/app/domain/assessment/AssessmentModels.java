package edu.ai.lms.core.quiz.app.domain.assessment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonRawValue;

import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;

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
            Integer variantCount,
            Integer optionCount,
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
            LocalDateTime updatedAt,
            AssessmentQuestionProgress questionProgress) {
        public AssessmentView(UUID id, UUID courseId, String title, String description, boolean unlimited,
                Integer timeLimitMinutes, Double passScore, LocalDateTime startTime, LocalDateTime dueTime,
                Integer attemptLimited, Integer variantCount, Integer optionCount, boolean allowRecentlyUsedQuestions,
                LifecycleStatus lifecycleStatus, int currentStep, UUID ccctProfileId, Long ccctProfileVersion,
                UUID examMatrixId, Long examMatrixVersion, boolean aiGenerationStarted, long version, UUID createdBy,
                UUID publishedBy, LocalDateTime publishedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this(id, courseId, title, description, unlimited, timeLimitMinutes, passScore, startTime, dueTime,
                    attemptLimited, variantCount, optionCount, allowRecentlyUsedQuestions, lifecycleStatus, currentStep,
                    ccctProfileId, ccctProfileVersion, examMatrixId, examMatrixVersion, aiGenerationStarted, version,
                    createdBy, publishedBy, publishedAt, createdAt, updatedAt, null);
        }
    }

    public record AssessmentQuestionProgress(
            Integer requiredPerVariant,
            int minCurrentPerVariant,
            int maxCurrentPerVariant,
            int variantCount,
            boolean complete) {
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
            String questionType,
            List<UUID> learningOutcomeIds,
            int required,
            int validated,
            int recentlyUsed,
            int reserved,
            int available,
            int missing) {
        public BankPlanCell(UUID matrixRowId, UUID customLearningLevelId, UUID chapterId,
                String chapter, String topic, String levelCode, String levelName,
                List<UUID> learningOutcomeIds, int required, int validated, int recentlyUsed,
                int reserved, int available, int missing) {
            this(matrixRowId, customLearningLevelId, chapterId, chapter, topic, levelCode, levelName,
                    "MULTIPLE_CHOICE", learningOutcomeIds, required, validated, recentlyUsed, reserved,
                    available, missing);
        }
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
            String questionType,
            List<UUID> learningOutcomeIds,
            int requiredCount,
            int generatedCount,
            String status,
            String errorMessage,
            int retryCount) {
    }

    public record GeneratedDraft(
            String content,
            RichContentFormat contentFormat,
            String explanation,
            RichContentFormat explanationFormat,
            String type,
            String codeRunnerContractJson,
            UUID matrixRowId,
            UUID customLearningLevelId,
            List<UUID> learningOutcomeIds,
            List<GeneratedAnswer> answers) {
        public GeneratedDraft(String content, String type, UUID matrixRowId, UUID customLearningLevelId,
                List<UUID> learningOutcomeIds, List<GeneratedAnswer> answers) {
            this(content, RichContentFormat.PLAIN_TEXT, null, RichContentFormat.PLAIN_TEXT,
                    type, null, matrixRowId, customLearningLevelId, learningOutcomeIds, answers);
        }
    }

    public record GeneratedAnswer(String content, RichContentFormat contentFormat, boolean correct,
            String explanation, RichContentFormat explanationFormat) {
        public GeneratedAnswer(String content, boolean correct, String explanation) {
            this(content, RichContentFormat.PLAIN_TEXT, correct, explanation, RichContentFormat.PLAIN_TEXT);
        }
    }

    public record DraftReviewItem(
            UUID id,
            UUID bankQuestionId,
            UUID generationBatchId,
            UUID matrixRowId,
            UUID customLearningLevelId,
            String content,
            RichContentFormat contentFormat,
            String explanation,
            RichContentFormat explanationFormat,
            String questionType,
            @JsonRawValue String codeRunnerContract,
            String cognitiveLevelCode,
            String cognitiveLevelName,
            UUID chapterId,
            String chapter,
            List<UUID> learningOutcomeIds,
            List<ReviewLearningOutcome> learningOutcomes,
            List<AnswerView> answers,
            String status,
            boolean recentlyUsed,
            long version,
            UUID reviewedBy,
            LocalDateTime reviewedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record ReviewLearningOutcome(UUID id, String label) {
    }

    public record AnswerView(UUID id, String content, RichContentFormat contentFormat,
            boolean correct, int orderIndex) {
        public AnswerView(UUID id, String content, boolean correct, int orderIndex) {
            this(id, content, RichContentFormat.PLAIN_TEXT, correct, orderIndex);
        }

        /** Source-compatible adapter for legacy callers; Quiz2 no longer exposes answer explanations. */
        public AnswerView(UUID id, String content, RichContentFormat contentFormat, boolean correct,
                String ignoredExplanation, RichContentFormat ignoredExplanationFormat, int orderIndex) {
            this(id, content, contentFormat, correct, orderIndex);
        }
    }

    public record DraftReviewView(
            UUID assessmentId,
            List<DraftReviewItem> notValidated,
            List<DraftReviewItem> validated,
            List<DraftReviewItem> rejected) {
    }

    public record DraftReviewPage(UUID assessmentId, List<DraftReviewItem> items, long total, int page, int size) {
    }

    public record DraftReviewSearch(
            String query,
            String status,
            UUID chapterId,
            UUID learningOutcomeId,
            String cognitiveLevelCode,
            UUID generationBatchId,
            String questionType,
            int page,
            int size) {
    }

    public record DraftReviewCriteria(
            UUID assessmentId,
            ReviewProgress progress,
            List<ReviewCriteriaCell> cells,
            ReviewFilterOptions filters) {
    }

    public record ReviewProgress(int generatedDrafts, int validatedDrafts, int eligibleBank, int required) {
    }

    public record ReviewCriteriaCell(
            UUID matrixRowId,
            UUID customLearningLevelId,
            UUID chapterId,
            String chapter,
            String topic,
            List<ReviewLearningOutcome> learningOutcomes,
            String questionType,
            String levelCode,
            String levelName,
            int required,
            int eligibleBank,
            int processing,
            int pendingDrafts,
            int validatedDrafts,
            int remaining) {
    }

    public record ReviewFilterOptions(
            List<FilterOption> statuses,
            List<FilterOption> chapters,
            List<FilterOption> learningOutcomes,
            List<FilterOption> cognitiveLevels,
            List<FilterOption> batches,
            List<FilterOption> questionTypes) {
    }

    public record FilterOption(String value, String label) {
    }

    public record DraftValidationResult(UUID id, String status, String errorMessage) {
    }

    public record BatchValidationResult(List<DraftValidationResult> results, int validatedCount, int failedCount) {
    }

    public record BaseGenerationView(
            UUID jobId,
            String status,
            String phase,
            Integer attemptCount,
            UUID quizId,
            String errorCode,
            String errorMessage,
            Integer generatedQuestionCount,
            Integer totalQuestionCount) {
    }

    public record VariantView(
            UUID id,
            UUID quizId,
            String code,
            String kind,
            int orderIndex,
            long shuffleSeed,
            String coverageStatus,
            String overrideReason,
            UUID overrideBy,
            LocalDateTime overrideAt,
            List<VariantQuestionView> questions) {
        public VariantView(UUID id, UUID quizId, String code, int orderIndex, long shuffleSeed,
                String coverageStatus, String overrideReason, UUID overrideBy, LocalDateTime overrideAt,
                List<VariantQuestionView> questions) {
            this(id, quizId, code, orderIndex == 0 ? "BASE" : "SHUFFLED", orderIndex, shuffleSeed,
                    coverageStatus, overrideReason, overrideBy, overrideAt, questions);
        }
    }

    public record VariantQuestionView(
            UUID id,
            UUID sourceBankQuestionId,
            String content,
            RichContentFormat contentFormat,
            String explanation,
            RichContentFormat explanationFormat,
            String type,
            VariantQuestionMetadata metadata,
            List<AnswerView> answers,
            int orderIndex,
            long version) {
        public VariantQuestionView(UUID id, UUID sourceBankQuestionId, String content,
                RichContentFormat contentFormat, String explanation, RichContentFormat explanationFormat,
                String type, VariantQuestionMetadata metadata, List<AnswerView> answers, int orderIndex) {
            this(id, sourceBankQuestionId, content, contentFormat, explanation, explanationFormat,
                    type, metadata, answers, orderIndex, 0);
        }

        public VariantQuestionView(UUID id, UUID sourceBankQuestionId, String content, String type,
                VariantQuestionMetadata metadata, List<AnswerView> answers, int orderIndex) {
            this(id, sourceBankQuestionId, content, RichContentFormat.PLAIN_TEXT, null,
                    RichContentFormat.PLAIN_TEXT, type, metadata, answers, orderIndex, 0);
        }


        public VariantQuestionView(UUID id, UUID sourceBankQuestionId, String content,
                RichContentFormat contentFormat, String type, VariantQuestionMetadata metadata,
                List<AnswerView> answers, int orderIndex) {
            this(id, sourceBankQuestionId, content, contentFormat, null,
                    RichContentFormat.PLAIN_TEXT, type, metadata, answers, orderIndex, 0);
        }
    }

    public record VariantQuestionMetadata(
            String matrixRow,
            String chapter,
            String topic,
            String customLearningLevelCode,
            String customLearningLevelName,
            List<ReviewLearningOutcome> learningOutcomes,
            String cognitiveLevelCode,
            String cognitiveLevelName) {
    }

    public record PublishResult(
            UUID assessmentId,
            LifecycleStatus status,
            int publishedVariants,
            int recentlyUsedQuestions,
            LocalDateTime publishedAt) {
    }
}
