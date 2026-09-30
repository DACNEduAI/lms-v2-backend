package edu.ai.lms.core.quiz3.app.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public final class Quiz3Models {
    private Quiz3Models() { }

    public enum SourceMode { USE_EXISTING, COPY_AS_DRAFT, CREATE_NEW_DRAFT }
    public enum MatrixMode { MANUAL, AUTOMATIC }
    public record Configuration(String title, String description, Boolean unlimited,
            Integer timeLimitMinutes, Double passScore, LocalDateTime startTime,
            LocalDateTime dueTime, Integer attemptLimited, Integer variantCount) { }
    public record AssessmentView(UUID id, UUID courseId, String title, String description,
            boolean unlimited, Integer timeLimitMinutes, Double passScore, LocalDateTime startTime,
            LocalDateTime dueTime, Integer attemptLimited, int variantCount, String lifecycleStatus,
            int currentStep, UUID ccctProfileId, LocalDateTime ccctSnapshotAt, UUID matrixId,
            MatrixMode matrixMode, UUID baseGenerationJobId, long version,
            LocalDateTime publishedAt, LocalDateTime createdAt, LocalDateTime updatedAt) { }
    public record SourceOption(UUID id, String name, String lifecycleStatus, MatrixMode matrixMode) { }
    public record ContextView(AssessmentView assessment, List<SourceOption> matrices) { }
    public record SourceSelection(SourceMode mode, UUID sourceId, String draftName, MatrixMode matrixMode) { }
    public record ContextSelection(SourceSelection ccct, SourceSelection matrix) { }
    public record SourceVersion(long version) { }
    public record VersionedConfiguration(long version, Configuration configuration) { }
    public record GenerationView(UUID jobId, String status, String phase, int attemptCount,
            UUID quizId, String errorCode, String errorMessage, int generatedQuestionCount,
            int totalQuestionCount, int processedVisualCount, int totalVisualCount, int failedVisualCount,
            List<String> visualWarnings) {
        public GenerationView(UUID jobId, String status, String phase, int attemptCount, UUID quizId,
                String errorCode, String errorMessage, int generatedQuestionCount, int totalQuestionCount) {
            this(jobId,status,phase,attemptCount,quizId,errorCode,errorMessage,generatedQuestionCount,totalQuestionCount,0,0,0,List.of());
        }
    }
    public record AnswerInput(String content, String contentFormat, boolean correct) { }
    public record QuestionUpdate(long expectedQuestionVersion, String content, String contentFormat,
            String explanation, String explanationFormat, List<AnswerInput> answers, List<ImageAssetSelection> imageAssetSelections) {
        public QuestionUpdate(long expectedQuestionVersion, String content, String contentFormat, String explanation, String explanationFormat, List<AnswerInput> answers) {
            this(expectedQuestionVersion,content,contentFormat,explanation,explanationFormat,answers,List.of());
        }
    }
    public record ImageAssetSelection(String token, UUID assetId) { }
    public record AnswerView(long number, String content, String contentFormat, boolean correct) { }
    public record QuestionMetadata(UUID matrixRowId, String chapter, UUID learningOutcomeId,
            String learningOutcome, UUID customLearningLevelId, String customLearningLevelName,
            String requestedAssetType, String effectiveAssetType, String qbank2SelectionMode,
            List<String> contentGroupIds, List<UUID> exampleIds, VisualMetadata visual) { }
    public record VisualMetadata(String status, String resolutionAction, UUID sourceAssetId, UUID generatedAssetId,
            String placement, Integer answerIndex, String errorMessage) { }
    public record QuestionView(UUID id, String content, String contentFormat, String explanation,
            String explanationFormat, String type, int orderIndex, long version,
            QuestionMetadata metadata, List<AnswerView> answers) { }
    public record VariantView(UUID id, UUID quizId, String code, String kind, int orderIndex,
            long shuffleSeed, String coverageStatus, List<QuestionView> questions) { }
    public record CreateVariantsRequest(int variantCount) { }
    public record PublishResult(UUID assessmentId, String status, int publishedVariants,
            LocalDateTime publishedAt) { }

    public record WebhookPayload(UUID eventId, UUID jobId, UUID assessmentId, String status,
            QuizPayload quiz, String errorCode, String errorMessage) { }
    public record QuizPayload(String quizName, List<GeneratedQuestion> questions) { }
    public record GeneratedQuestion(String content, String type, UUID matrixRowId,
            UUID customLearningLevelId, UUID learningOutcomeId, List<GeneratedAnswer> answers,
            Map<String, Object> qbank2Reference, JsonNode codeRunnerContract,
            String assetType, String effectiveAssetType, String assetFallbackReason, Map<String,Object> visual,
            Map<String,Object> assetResolution) {
        public GeneratedQuestion(String content, String type, UUID matrixRowId,
                UUID customLearningLevelId, UUID learningOutcomeId, List<GeneratedAnswer> answers,
                Map<String, Object> qbank2Reference, JsonNode codeRunnerContract) {
            this(content,type,matrixRowId,customLearningLevelId,learningOutcomeId,answers,qbank2Reference,codeRunnerContract,"TEXT","TEXT",null,null,null);
        }
        public GeneratedQuestion(String content, String type, UUID matrixRowId, UUID customLearningLevelId, UUID learningOutcomeId, List<GeneratedAnswer> answers, Map<String,Object> qbank2Reference, JsonNode codeRunnerContract, String assetType, String effectiveAssetType, String assetFallbackReason) {
            this(content,type,matrixRowId,customLearningLevelId,learningOutcomeId,answers,qbank2Reference,codeRunnerContract,assetType,effectiveAssetType,assetFallbackReason,null,null);
        }
    }
    public record GeneratedAnswer(String content, boolean isCorrect, String explanation) { }
    public record ProgressPayload(UUID jobId, UUID assessmentId, int attempt,
            int generatedQuestionCount) { }
    public record AiAnchor(String kind, Integer offset) { }
    public record AiRequest(String mode, String prompt, long expectedQuestionVersion, String currentContent,
            String currentContentFormat, AiAnchor anchor) { }
    public record AiDraft(UUID questionId, long baseQuestionVersion, String content,
            String contentFormat, int appliedChangeIndex, int latestChangeIndex,
            boolean canUndo, boolean canRedo) { }
    public record AiStep(String actionId, String specialist, String status, String errorCode, String errorMessage) { }
    public record AiJobView(UUID id, String status, String phase, String errorCode, String errorMessage,
            String warningMessage, List<AiStep> steps) { }
    public record AiStartResult(AiJobView job, boolean existing) { }
    public record QBank2RegenerateRequest(List<UUID> exampleIds) { }
    public record QBank2RegenerationJobView(UUID id, String status, String phase, String errorCode, String errorMessage) { }
    public record QBank2RegenerationStartResult(QBank2RegenerationJobView job, boolean existing) { }
    public record QBank2RegenerationDraft(UUID questionId, long baseQuestionVersion, String content, String contentFormat,
            String explanation, String explanationFormat, List<AnswerInput> answers, List<String> contentGroupIds,
            List<UUID> exampleIds) { }
}
