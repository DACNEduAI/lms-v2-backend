package edu.ai.lms.core.quiz.app.domain.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class QuestionAiModels {
    private QuestionAiModels() {}

    public enum Mode { ORCHESTRATE, TEXT, LATEX, SVG }
    public enum Status { QUEUED, RUNNING, NEEDS_CLARIFICATION, APPLIED_TO_WORKING_DRAFT, FAILED, CANCELLED, TIMED_OUT }
    public enum Phase { QUEUED, ORCHESTRATOR, TEXT, LATEX, SVG, VALIDATOR, CRITIC, APPLYING, DONE }
    public enum ArtifactType { SVG, LATEX }

    public record Anchor(String kind, String blockId, Integer offset) {
        public Anchor {
            kind = kind == null || kind.isBlank() ? "END" : kind;
            if (!List.of("CURSOR", "END").contains(kind)) throw new IllegalArgumentException("AI insertion anchor is invalid");
            if (offset != null && offset < 0) throw new IllegalArgumentException("AI insertion offset cannot be negative");
        }
    }

    public record StartJobRequest(
            Mode mode,
            String prompt,
            Anchor anchor,
            UUID artifactId,
            String operation,
            Long expectedQuestionVersion,
            String currentContent,
            RichContentFormat currentContentFormat,
            String legacySource,
            String legacyLatexMode) {}

    public record ClarificationRequest(String answer) {}

    public record StructuredRenderRequest(UUID artifactId, JsonNode spec, JsonNode layoutOverride,
            String altText, Long expectedQuestionVersion) {}

    public record JobStepView(
            UUID id,
            String actionId,
            int stepOrder,
            String specialist,
            String status,
            int attempt,
            JsonNode input,
            JsonNode output,
            String errorMessage,
            LocalDateTime startedAt,
            LocalDateTime completedAt) {}

    public record JobView(
            UUID id,
            UUID assessmentId,
            UUID variantId,
            UUID questionId,
            Mode mode,
            Status status,
            Phase phase,
            String prompt,
            JsonNode actionPlan,
            long baseQuestionVersion,
            String clarificationQuestion,
            String errorCode,
            String errorMessage,
            String warningMessage,
            int actionCount,
            LocalDateTime createdAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            List<JobStepView> steps) {}

    public record ArtifactView(
            UUID id,
            ArtifactType type,
            int revision,
            String sourceCode,
            String latexMode,
            String visualKind,
            String generator,
            JsonNode spec,
            JsonNode layoutOverride,
            String prompt,
            JsonNode critic,
            JsonNode fallbackMetadata,
            LocalDateTime createdAt) {}

    public record WorkingDraftView(
            UUID questionId,
            long baseQuestionVersion,
            String content,
            RichContentFormat contentFormat,
            int appliedChangeIndex,
            int latestChangeIndex,
            boolean canUndo,
            boolean canRedo,
            List<ArtifactView> artifacts) {}

    public record TimelineView(List<JobView> jobs) {}
}
