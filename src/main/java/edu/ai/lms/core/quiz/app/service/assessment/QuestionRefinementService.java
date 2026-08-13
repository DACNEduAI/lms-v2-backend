package edu.ai.lms.core.quiz.app.service.assessment;

import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import edu.ai.lms.config.CorrelationIdFilter;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository.AnswerInput;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantQuestionMetadata;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.VariantEditingContext;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.VariantQuestionInput;
import edu.ai.lms.core.quiz.app.service.genquiz.QuestionVisualGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionRefinementService {
    private static final Set<Target> VISUAL_TARGETS = Set.of(Target.QUESTION, Target.ANSWER_A, Target.ANSWER_B,
            Target.ANSWER_C, Target.ANSWER_D, Target.ANSWER_E);
    private static final Pattern IMAGE_SOURCE = Pattern.compile("<img\\b[^>]*\\bsrc\\s*=\\s*(['\"])(.*?)\\1",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IMAGE_TAG = Pattern.compile("<img\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_TAG = Pattern.compile("<svg\\b", Pattern.CASE_INSENSITIVE);

    private final AssessmentService assessments;
    private final WebClient aiWebClient;

    @Value("${quiz.refinement.request-timeout-seconds:120}")
    private long timeoutSeconds;

    public RefinementResponse refine(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId,
            RefinementRequest request) {
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            throw new IllegalArgumentException("A refinement prompt is required");
        }
        if (request.prompt().length() > 4000) throw new IllegalArgumentException("The refinement prompt is too long");
        if (request.messages() != null && request.messages().size() > 20) {
            throw new IllegalArgumentException("A refinement session supports at most 20 messages");
        }
        if (request.target() == null) throw new IllegalArgumentException("A refinement target is required");
        if (request.messages() != null) request.messages().forEach(message -> {
            if (message == null || !("user".equals(message.role()) || "assistant".equals(message.role()))
                    || message.content() == null || message.content().isBlank() || message.content().length() > 4000) {
                throw new IllegalArgumentException("Refinement messages are invalid");
            }
        });
        VariantEditingContext context = assessments.requireVariantEditingContext(courseId, assessmentId, variantId, questionId);
        VariantQuestionInput original = assessments.sanitizeVariantQuestionInput(request.draft(), context.assessment().optionCount());
        AiRequest aiRequest = new AiRequest(request.prompt(), request.target(), original,
                request.messages() == null ? List.of() : request.messages(), metadata(context.question().metadata()));
        AiResponse response = callAi(aiRequest, request.target());
        if (response == null) throw new IllegalStateException("AI refinement returned no result");
        if (response.draft() == null) throw new IllegalArgumentException("AI refinement returned no draft");
        if (containsInlineSvg(response.draft())) {
            throw new IllegalArgumentException("AI visuals must use the pending SVG contract");
        }
        if (!imageSources(original).equals(imageSources(response.draft()))) {
            throw new IllegalArgumentException("AI may not insert external or inline image URLs");
        }
        VariantQuestionInput refined = assessments.sanitizeVariantQuestionInput(response.draft(), context.assessment().optionCount());
        validateTarget(request.target(), original, refined);
        Visual visual = validateVisual(response.visual(), request.target(), context.assessment().optionCount());
        List<Target> changedTargets = changedTargets(original, refined, visual);
        return new RefinementResponse(response.assistantMessage(), changedTargets, refined, visual);
    }

    private AiResponse callAi(AiRequest request, Target target) {
        String correlationId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        try {
            AiResponse response = aiWebClient.post().uri("/quiz-gen/questions/refine")
                    .header(CorrelationIdFilter.HEADER_NAME, correlationId)
                    .bodyValue(request).retrieve().bodyToMono(AiResponse.class)
                    .block(Duration.ofSeconds(Math.max(1, timeoutSeconds)));
            log.info("event=question.refine correlation_id={} duration_ms={} target={} result=SUCCESS",
                    correlationId, Duration.ofNanos(System.nanoTime() - started).toMillis(), target);
            return response;
        } catch (RuntimeException exception) {
            log.warn("event=question.refine correlation_id={} duration_ms={} target={} result=FAILED error_type={}",
                    correlationId, Duration.ofNanos(System.nanoTime() - started).toMillis(), target,
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private QuestionRefinementContext metadata(VariantQuestionMetadata value) {
        return new QuestionRefinementContext(value.matrixRow(), value.chapter(), value.topic(), value.customLearningLevelCode(),
                value.customLearningLevelName(), value.learningOutcomes().stream().map(outcome -> outcome.label()).toList(),
                value.cognitiveLevelCode(), value.cognitiveLevelName());
    }

    private void validateTarget(Target target, VariantQuestionInput before, VariantQuestionInput after) {
        if (target == Target.AUTO || target == Target.ALL) return;
        if (target != Target.QUESTION && (!Objects.equals(before.content(), after.content())
                || before.contentFormat() != after.contentFormat())) {
            throw new IllegalArgumentException("AI changed the question outside the selected target");
        }
        if (target != Target.EXPLANATION && (!Objects.equals(before.explanation(), after.explanation())
                || before.explanationFormat() != after.explanationFormat())) {
            throw new IllegalArgumentException("AI changed the explanation outside the selected target");
        }
        int answerTarget = answerIndex(target);
        for (int index = 0; index < before.answers().size(); index++) {
            AnswerInput left = before.answers().get(index);
            AnswerInput right = after.answers().get(index);
            if (index != answerTarget && (!Objects.equals(left.content(), right.content())
                    || left.contentFormat() != right.contentFormat())) {
                throw new IllegalArgumentException("AI changed an answer outside the selected target");
            }
            if (answerTarget < 0 && left.correct() != right.correct()) {
                throw new IllegalArgumentException("AI changed the correct answer outside the selected answer target");
            }
        }
    }

    private List<String> imageSources(VariantQuestionInput input) {
        List<String> sources = new java.util.ArrayList<>();
        List<String> values = new java.util.ArrayList<>();
        values.add(input.content());
        values.add(input.explanation());
        input.answers().forEach(answer -> values.add(answer.content()));
        int imageTags = values.stream().filter(Objects::nonNull)
                .mapToInt(value -> (int) IMAGE_TAG.matcher(value).results().count()).sum();
        sources.add("__image_tags__=" + imageTags);
        values.stream().filter(Objects::nonNull).forEach(value -> {
            var matcher = IMAGE_SOURCE.matcher(value);
            while (matcher.find()) sources.add(matcher.group(2));
        });
        sources.sort(String::compareTo);
        return sources;
    }

    private boolean containsInlineSvg(VariantQuestionInput input) {
        if (SVG_TAG.matcher(Objects.toString(input.content(), "")).find()
                || SVG_TAG.matcher(Objects.toString(input.explanation(), "")).find()) return true;
        return input.answers().stream().anyMatch(answer -> SVG_TAG.matcher(Objects.toString(answer.content(), "")).find());
    }

    private Visual validateVisual(Visual visual, Target requestedTarget, int optionCount) {
        if (visual == null) return null;
        if (!VISUAL_TARGETS.contains(visual.target())) throw new IllegalArgumentException("SVG target is not supported");
        int answerIndex = answerIndex(visual.target());
        if (answerIndex >= optionCount) throw new IllegalArgumentException("SVG target exceeds the configured answer count");
        if (requestedTarget != Target.AUTO && requestedTarget != Target.ALL && requestedTarget != visual.target()) {
            throw new IllegalArgumentException("SVG was generated outside the selected target");
        }
        if (!"image/svg+xml".equals(visual.renderedMediaType())) {
            throw new IllegalArgumentException("Interactive refinement only accepts SVG");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(visual.renderedImageBase64());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Generated SVG is not valid base64", exception);
        }
        if (decoded.length == 0 || decoded.length > 200_000) throw new IllegalArgumentException("Generated SVG is empty or too large");
        String decodedSource = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        String sanitizedSource = QuestionVisualGenerationService.sanitizeInteractiveSvg(visual.sourceCode());
        String sanitizedDecoded = QuestionVisualGenerationService.sanitizeInteractiveSvg(decodedSource);
        if (!sanitizedSource.equals(sanitizedDecoded)) {
            throw new IllegalArgumentException("Generated SVG source and rendered payload do not match");
        }
        String canonicalBase64 = Base64.getEncoder().encodeToString(
                sanitizedSource.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new Visual(visual.id(), visual.target(), visual.altText(), sanitizedSource, canonicalBase64,
                visual.renderedMediaType());
    }

    private List<Target> changedTargets(VariantQuestionInput before, VariantQuestionInput after, Visual visual) {
        Set<Target> changed = new LinkedHashSet<>();
        if (!Objects.equals(before.content(), after.content()) || before.contentFormat() != after.contentFormat()) {
            changed.add(Target.QUESTION);
        }
        if (!Objects.equals(before.explanation(), after.explanation())
                || before.explanationFormat() != after.explanationFormat()) {
            changed.add(Target.EXPLANATION);
        }
        for (int index = 0; index < before.answers().size(); index++) {
            AnswerInput left = before.answers().get(index);
            AnswerInput right = after.answers().get(index);
            if (!Objects.equals(left.content(), right.content()) || left.contentFormat() != right.contentFormat()
                    || left.correct() != right.correct()) {
                changed.add(answerTarget(index));
            }
        }
        if (visual != null) changed.add(visual.target());
        return List.copyOf(changed);
    }

    private Target answerTarget(int index) {
        return switch (index) {
            case 0 -> Target.ANSWER_A;
            case 1 -> Target.ANSWER_B;
            case 2 -> Target.ANSWER_C;
            case 3 -> Target.ANSWER_D;
            case 4 -> Target.ANSWER_E;
            default -> throw new IllegalArgumentException("Unsupported answer index");
        };
    }

    private int answerIndex(Target target) {
        return switch (target) {
            case ANSWER_A -> 0;
            case ANSWER_B -> 1;
            case ANSWER_C -> 2;
            case ANSWER_D -> 3;
            case ANSWER_E -> 4;
            default -> -1;
        };
    }

    public enum Target { AUTO, QUESTION, ANSWER_A, ANSWER_B, ANSWER_C, ANSWER_D, ANSWER_E, EXPLANATION, ALL }

    public record Message(String role, String content) {}
    public record RefinementRequest(String prompt, Target target, VariantQuestionInput draft, List<Message> messages) {}
    private record AiRequest(String prompt, Target target, VariantQuestionInput draft, List<Message> messages,
            QuestionRefinementContext metadata) {}
    private record AiResponse(String assistantMessage, List<Target> changedTargets, VariantQuestionInput draft, Visual visual) {}
    public record Visual(UUID id, Target target, String altText, String sourceCode, String renderedImageBase64,
            String renderedMediaType) {}
    public record RefinementResponse(String assistantMessage, List<Target> changedTargets,
            VariantQuestionInput draft, Visual visual) {}
}
