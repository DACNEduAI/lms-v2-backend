package edu.ai.lms.core.quiz3;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class Quiz3Models {
    private Quiz3Models() { }

    public enum SourceMode { USE_EXISTING, COPY_AS_DRAFT, CREATE_NEW_DRAFT }
    public record Configuration(String title, String description, Boolean unlimited,
            Integer timeLimitMinutes, Double passScore, LocalDateTime startTime,
            LocalDateTime dueTime, Integer attemptLimited, Integer variantCount) { }
    public record AssessmentView(UUID id, UUID courseId, String title, String description,
            boolean unlimited, Integer timeLimitMinutes, Double passScore, LocalDateTime startTime,
            LocalDateTime dueTime, Integer attemptLimited, int variantCount, String lifecycleStatus,
            int currentStep, UUID ccctProfileId, Long ccctProfileVersion, UUID matrix2Id,
            Long matrix2Version, UUID baseGenerationJobId, long version,
            LocalDateTime publishedAt, LocalDateTime createdAt, LocalDateTime updatedAt) { }
    public record SourceOption(UUID id, String name, String lifecycleStatus, long version) { }
    public record ContextView(AssessmentView assessment, List<SourceOption> ccctProfiles,
            List<SourceOption> matrices) { }
    public record SourceSelection(SourceMode mode, UUID sourceId, String draftName) { }
    public record ContextSelection(SourceSelection ccct, SourceSelection matrix2) { }
    public record SourceVersion(long version) { }
    public record VersionedConfiguration(long version, Configuration configuration) { }
    public record GenerationView(UUID jobId, String status, String phase, int attemptCount,
            UUID quizId, String errorCode, String errorMessage, int generatedQuestionCount,
            int totalQuestionCount) { }
    public record AnswerInput(String content, String contentFormat, boolean correct) { }
    public record QuestionUpdate(long expectedQuestionVersion, String content, String contentFormat,
            String explanation, String explanationFormat, List<AnswerInput> answers) { }
    public record AnswerView(long number, String content, String contentFormat, boolean correct) { }
    public record QuestionMetadata(UUID matrixRowId, String chapter, UUID learningOutcomeId,
            String learningOutcome, UUID customLearningLevelId, String customLearningLevelName) { }
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
            UUID customLearningLevelId, UUID learningOutcomeId, List<GeneratedAnswer> answers) { }
    public record GeneratedAnswer(String content, boolean isCorrect, String explanation) { }
    public record ProgressPayload(UUID jobId, UUID assessmentId, int attempt,
            int generatedQuestionCount) { }
    public record AiRequest(String mode, String prompt, long expectedQuestionVersion, String currentContent,
            String currentContentFormat) { }
    public record AiDraft(UUID questionId, long baseQuestionVersion, String content,
            String contentFormat, int appliedChangeIndex, int latestChangeIndex,
            boolean canUndo, boolean canRedo) { }
}
