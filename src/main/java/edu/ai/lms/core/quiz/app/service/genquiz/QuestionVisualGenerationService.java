package edu.ai.lms.core.quiz.app.service.genquiz;

import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionImagePublicUrlPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.QuestionVisualJobPort;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuestionVisualJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.UploadQuestionImagePort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

@Service
@Slf4j
public class QuestionVisualGenerationService {
    private static final int MAX_ATTEMPTS = 3;
    private static final Pattern LATEX_UNSAFE = Pattern.compile(
            "\\\\(?:href|url|includegraphics|htmlClass|htmlId|htmlStyle|htmlData)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Set<String> SVG_ELEMENTS = Set.of(
            "svg", "g", "path", "rect", "circle", "ellipse", "line", "polyline", "polygon",
            "text", "tspan", "defs", "marker", "title", "desc");
    private static final Set<String> SVG_ATTRIBUTES = Set.of(
            "viewBox", "width", "height", "x", "y", "x1", "y1", "x2", "y2", "cx", "cy",
            "r", "rx", "ry", "d", "points", "fill", "stroke", "stroke-width", "stroke-linecap",
            "stroke-linejoin", "opacity", "transform", "font-size", "font-family", "text-anchor",
            "dominant-baseline", "marker-start", "marker-mid", "marker-end", "id", "class", "role",
            "aria-label", "xmlns");

    private final QuestionVisualJobPort visualJobs;
    private final LoadQuizGenerationJobPort loadParent;
    private final SaveQuizGenerationJobPort saveParent;
    private final UploadQuestionImagePort uploadImage;
    private final LoadQuestionImagePublicUrlPort publicUrl;
    private final WebClient aiWebClient;
    private final SseQuizGenerationService sse;
    private final LoadSignedInUserPort signedInUser;
    private final ScheduleQuestionVisualJobPort visualScheduler;
    private final int maxConcurrency;
    private final long generationTimeoutMinutes;
    private final Duration requestTimeout;
    private final Duration leaseDuration;
    private final ThreadPoolTaskExecutor visualExecutor;
    private final AtomicInteger inFlight = new AtomicInteger();

    public QuestionVisualGenerationService(
            QuestionVisualJobPort visualJobs,
            LoadQuizGenerationJobPort loadParent,
            SaveQuizGenerationJobPort saveParent,
            UploadQuestionImagePort uploadImage,
            LoadQuestionImagePublicUrlPort publicUrl,
            WebClient aiWebClient,
            SseQuizGenerationService sse,
            LoadSignedInUserPort signedInUser,
            ScheduleQuestionVisualJobPort visualScheduler,
            @Value("${quiz.visual.max-concurrency:3}") int maxConcurrency,
            @Value("${quiz.generation.timeout-minutes:20}") long generationTimeoutMinutes,
            @Value("${quiz.visual.request-timeout-seconds:120}") long requestTimeoutSeconds,
            @Value("${quiz.visual.lease-safety-margin-seconds:30}") long leaseSafetyMarginSeconds,
            @Qualifier("quizVisualGenerationExecutor") ThreadPoolTaskExecutor visualExecutor) {
        if (maxConcurrency < 1 || generationTimeoutMinutes < 1
                || requestTimeoutSeconds < 1 || leaseSafetyMarginSeconds < 1) {
            throw new IllegalArgumentException("Quiz visual concurrency and timeout settings must be positive");
        }
        this.visualJobs = visualJobs;
        this.loadParent = loadParent;
        this.saveParent = saveParent;
        this.uploadImage = uploadImage;
        this.publicUrl = publicUrl;
        this.aiWebClient = aiWebClient;
        this.sse = sse;
        this.signedInUser = signedInUser;
        this.visualScheduler = visualScheduler;
        this.maxConcurrency = maxConcurrency;
        this.generationTimeoutMinutes = generationTimeoutMinutes;
        this.requestTimeout = Duration.ofSeconds(requestTimeoutSeconds);
        this.leaseDuration = requestTimeout.plusSeconds(leaseSafetyMarginSeconds);
        this.visualExecutor = visualExecutor;
    }

    public void requestDispatchAfterCommit(UUID parentJobId, LocalDateTime parentCreatedAt) {
        Runnable schedule = () -> scheduleParent(parentJobId, parentCreatedAt);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            schedule.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                schedule.run();
            }
        });
    }

    private void scheduleParent(UUID parentJobId, LocalDateTime parentCreatedAt) {
        LocalDateTime startedAt = parentCreatedAt == null ? LocalDateTime.now() : parentCreatedAt;
        visualScheduler.scheduleImmediate(parentJobId, LocalDateTime.now());
        visualScheduler.scheduleDeadline(parentJobId, startedAt.plusMinutes(generationTimeoutMinutes));
    }

    public void requestDispatch() {
        dispatchAvailable();
    }

    synchronized void dispatchAvailable() {
        int available = Math.max(0, maxConcurrency - inFlight.get());
        if (available == 0) return;
        LocalDateTime leaseUntil = LocalDateTime.now().plus(leaseDuration);
        List<QuestionVisualJobPort.WorkItem> claimed = visualJobs.claimAvailable(available, leaseUntil);
        for (QuestionVisualJobPort.WorkItem work : claimed) {
            try {
                visualScheduler.scheduleLeaseRecovery(work.id(), leaseUntil);
            } catch (RuntimeException ex) {
                LocalDateTime retryAt = LocalDateTime.now().plusSeconds(1);
                if (visualJobs.retry(work.id(), "SCHEDULER_ERROR", "Visual scheduling will be retried", retryAt)) {
                    scheduleRetryOrFail(work, retryAt);
                }
                continue;
            }
            inFlight.incrementAndGet();
            try {
                visualExecutor.execute(() -> runWorker(work));
            } catch (RejectedExecutionException ex) {
                inFlight.decrementAndGet();
                LocalDateTime retryAt = LocalDateTime.now().plusSeconds(1);
                if (visualJobs.retry(work.id(), "WORKER_BUSY", "Visual worker is busy", retryAt)) {
                    scheduleRetryOrFail(work, retryAt);
                }
            }
        }
    }

    private void runWorker(QuestionVisualJobPort.WorkItem work) {
        try {
            process(work);
        } finally {
            inFlight.decrementAndGet();
            try {
                requestDispatch();
            } catch (RuntimeException ex) {
                log.warn("Could not refill question visual workers after job {}; scheduling a one-time wake-up",
                        work.id(), ex);
                try {
                    visualScheduler.scheduleImmediate(work.parentJobId(), LocalDateTime.now().plusSeconds(1));
                } catch (RuntimeException schedulingFailure) {
                    log.error("Could not schedule the question visual refill wake-up for parent {}",
                            work.parentJobId(), schedulingFailure);
                }
            }
        }
    }

    void process(QuestionVisualJobPort.WorkItem work) {
        try {
            if (work.parentCreatedAt().plusMinutes(generationTimeoutMinutes).isBefore(LocalDateTime.now())) {
                expireParent(work.parentJobId());
                return;
            }
            if (!Set.of("LATEX", "SVG").contains(work.visualType())) {
                terminalFailure(work, "UNSUPPORTED_VISUAL_TYPE", "Only LATEX and SVG are supported");
                return;
            }
            VisualResponse response = requestVisual(work);
            if (response == null || !work.id().equals(response.visualJobId())
                    || !work.visualType().equals(response.visualType())) {
                throw new UnsafeSourceException("Visual response identity or type is invalid");
            }
            String source = "LATEX".equals(work.visualType())
                    ? validateLatex(response.sourceCode()) : sanitizeSvg(response.sourceCode());
            byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
            String hash = sha256(bytes);
            boolean completed;
            if ("LATEX".equals(work.visualType())) {
                completed = visualJobs.completeLatex(work.id(), work.imageId(), source, hash, bytes.length);
            } else {
                String key = "quizzes/" + work.quizId() + "/question-images/" + work.imageId() + "-"
                        + hash.substring(0, 12) + ".svg";
                uploadImage.uploadQuestionImage(key, bytes, "image/svg+xml", true);
                completed = visualJobs.completeSvg(work.id(), work.imageId(), source, key,
                        publicUrl.getPublicImageUrl(key), hash, bytes.length);
            }
            if (!completed) return;
            cancelChildSafely(work.id());
            pushParentProgress(work.parentJobId());
            finalizeParentIfTerminal(work.parentJobId());
        } catch (IllegalArgumentException ex) {
            terminalFailure(work, "UNSAFE_VISUAL_SOURCE", ex.getMessage());
        } catch (Exception ex) {
            int attempted = work.attemptCount() + 1;
            if (attempted < MAX_ATTEMPTS) {
                LocalDateTime retryAt = LocalDateTime.now().plusSeconds(1L << attempted);
                if (visualJobs.retry(work.id(), "VISUAL_SERVICE_ERROR", "Visual generation will be retried", retryAt)) {
                    scheduleRetryOrFail(work, retryAt);
                }
            } else {
                terminalFailure(work, "VISUAL_SERVICE_ERROR", "Visual generation failed after three attempts");
            }
        }
    }

    VisualResponse requestVisual(QuestionVisualJobPort.WorkItem work) {
        return aiWebClient.post().uri("/quiz-gen/visuals/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new VisualRequest(work.id(), work.visualType(), work.questionContent(),
                        work.generationBrief(), work.altText()))
                .retrieve().bodyToMono(VisualResponse.class).block(requestTimeout);
    }

    private void terminalFailure(QuestionVisualJobPort.WorkItem work, String code, String message) {
        if (!visualJobs.fail(work.id(), work.imageId(), code, message)) return;
        cancelChildSafely(work.id());
        pushParentProgress(work.parentJobId());
        finalizeParentIfTerminal(work.parentJobId());
    }

    public void expireParent(UUID parentId) {
        String message = "Visual generation exceeded the overall quiz-generation deadline";
        QuestionVisualJobPort.DeadlineResult result = visualJobs.expireParent(
                parentId, "VISUAL_DEADLINE_EXCEEDED", message);
        if (!result.applied()) return;
        result.jobIds().forEach(this::cancelChildSafely);
        cancelParentSafely(parentId);
        QuizGenerationJob parent = loadParent.getById(parentId).orElse(null);
        if (parent == null) return;
        var response = QuizGenerationService.toStatusResponse(parent);
        response.setVisualSummary(new edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse.VisualSummary(
                result.summary().requested(), result.summary().ready(), result.summary().failed()));
        sse.pushStatus(parentId, response);
    }

    public void reconcileSchedules() {
        LocalDateTime now = LocalDateTime.now();
        Set<UUID> deadlines = new HashSet<>();
        for (QuestionVisualJobPort.RecoveryItem item : visualJobs.findActiveForScheduling()) {
            if (deadlines.add(item.parentJobId())) {
                visualScheduler.scheduleDeadline(item.parentJobId(),
                        item.parentCreatedAt().plusMinutes(generationTimeoutMinutes));
            }
            if ("PROCESSING".equals(item.status())) {
                LocalDateTime fireAt = item.leaseUntil() == null || item.leaseUntil().isBefore(now)
                        ? now : item.leaseUntil();
                visualScheduler.scheduleLeaseRecovery(item.jobId(), fireAt);
            } else {
                LocalDateTime fireAt = item.nextAttemptAt() == null || item.nextAttemptAt().isBefore(now)
                        ? now : item.nextAttemptAt();
                visualScheduler.scheduleRetry(item.jobId(), fireAt);
            }
        }
    }

    private void pushParentProgress(UUID parentId) {
        QuizGenerationJob parent = loadParent.getById(parentId).orElse(null);
        if (parent == null) return;
        QuestionVisualJobPort.Summary summary = visualJobs.summarize(parentId);
        var response = QuizGenerationService.toStatusResponse(parent);
        response.setVisualSummary(new edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse.VisualSummary(
                summary.requested(), summary.ready(), summary.failed()));
        sse.pushStatus(parentId, response);
    }

    private void finalizeParentIfTerminal(UUID parentId) {
        QuestionVisualJobPort.Summary summary = visualJobs.summarize(parentId);
        if (summary.active() > 0) return;
        QuizGenerationJob parent = loadParent.getById(parentId).orElse(null);
        if (parent == null || parent.getStatus() != QuizGenerationStatus.PROCESSING) return;
        parent.setPhase(QuizGenerationJob.Phase.FINALIZING);
        saveParent.save(parent);
        parent.setStatus(QuizGenerationStatus.COMPLETED);
        parent.setPhase(QuizGenerationJob.Phase.DONE);
        parent.setLeaseUntil(null);
        parent.setCompletedAt(LocalDateTime.now());
        if (summary.failed() > 0) {
            parent.setErrorCode("VISUALS_PARTIALLY_FAILED");
            parent.setErrorMessage(summary.failed() + " required visual(s) failed; the hidden quiz remains reviewable");
        }
        QuizGenerationJob saved = saveParent.save(parent);
        cancelParentSafely(parentId);
        var response = QuizGenerationService.toStatusResponse(saved);
        response.setVisualSummary(new edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse.VisualSummary(
                summary.requested(), summary.ready(), summary.failed()));
        sse.pushStatus(parentId, response);
    }

    static String validateLatex(String source) {
        if (source == null || source.isBlank() || source.length() > 10_000) {
            throw new UnsafeSourceException("LaTeX source is empty or exceeds 10,000 characters");
        }
        String value = source.trim();
        String lower = value.toLowerCase();
        if (value.startsWith("```") || value.contains("$") || value.contains("<")
                || lower.contains("\\begin{document}") || LATEX_UNSAFE.matcher(value).find()) {
            throw new UnsafeSourceException("LaTeX contains forbidden delimiters, HTML, or external-resource commands");
        }
        return value;
    }

    static String sanitizeSvg(String source) {
        if (source == null || source.isBlank() || source.getBytes(StandardCharsets.UTF_8).length > 200_000
                || source.toLowerCase().contains("<!doctype") || source.toLowerCase().contains("<!entity")) {
            throw new UnsafeSourceException("SVG source is empty, oversized, or contains entities");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(source)));
            Element root = document.getDocumentElement();
            if (!"svg".equals(root.getLocalName() == null ? root.getTagName() : root.getLocalName())
                    || !root.hasAttribute("viewBox")) {
                throw new UnsafeSourceException("SVG requires one root element with a viewBox");
            }
            String[] viewBox = root.getAttribute("viewBox").trim().split("[ ,]+");
            if (viewBox.length != 4) throw new UnsafeSourceException("SVG viewBox must have four numbers");
            double width = Double.parseDouble(viewBox[2]);
            double height = Double.parseDouble(viewBox[3]);
            if (!Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0
                    || width > 10_000 || height > 10_000) {
                throw new UnsafeSourceException("SVG viewBox dimensions are invalid or too large");
            }
            int[] count = {0};
            validateSvgNode(root, count);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            var transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (UnsafeSourceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnsafeSourceException("SVG is not safe, valid XML", ex);
        }
    }

    private static void validateSvgNode(Element element, int[] count) {
        if (++count[0] > 1000) throw new UnsafeSourceException("SVG contains too many nodes");
        String tag = element.getLocalName() == null ? element.getTagName() : element.getLocalName();
        if (!SVG_ELEMENTS.contains(tag)) throw new UnsafeSourceException("SVG element is not allowed: " + tag);
        var attributes = element.getAttributes();
        for (int index = 0; index < attributes.getLength(); index++) {
            Node attribute = attributes.item(index);
            String name = attribute.getLocalName() == null ? attribute.getNodeName() : attribute.getLocalName();
            String value = attribute.getNodeValue().toLowerCase();
            if (name.toLowerCase().startsWith("on") || !SVG_ATTRIBUTES.contains(name)
                    || value.contains("javascript:") || value.contains("data:")
                    || value.contains("http:") || value.contains("https:")
                    || (value.contains("url(") && !value.matches("url\\(#[a-z_][\\w.-]*\\)"))) {
                throw new UnsafeSourceException("SVG attribute is not allowed: " + name);
            }
        }
        if (element.getTextContent() != null && element.getTextContent().length() > 20_000) {
            throw new UnsafeSourceException("SVG text content is too long");
        }
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element childElement) validateSvgNode(childElement, count);
        }
    }

    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    public void retryFailed(UUID quizId, UUID questionId) {
        UUID requestedBy = signedInUser.getAuthenticatedUser()
                .orElseThrow(() -> new IllegalArgumentException("User is not authenticated")).getId();
        QuestionVisualJobPort.RetryItem retry = visualJobs.retryFailed(quizId, questionId, requestedBy)
                .orElseThrow(() -> new IllegalArgumentException("No failed visual generation job exists for this question"));
        Runnable schedule = () -> {
            visualScheduler.scheduleRetry(retry.jobId(), LocalDateTime.now());
            visualScheduler.scheduleDeadline(retry.parentJobId(),
                    retry.parentCreatedAt().plusMinutes(generationTimeoutMinutes));
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            schedule.run();
        } else {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { schedule.run(); }
            });
        }
    }

    private void scheduleRetryOrFail(QuestionVisualJobPort.WorkItem work, LocalDateTime retryAt) {
        try {
            visualScheduler.scheduleRetry(work.id(), retryAt);
        } catch (RuntimeException ex) {
            log.error("Could not schedule retry for question visual job {}", work.id(), ex);
            terminalFailure(work, "VISUAL_SCHEDULER_ERROR", "Visual retry could not be scheduled");
        }
    }

    private void cancelChildSafely(UUID jobId) {
        try {
            visualScheduler.cancelChild(jobId);
        } catch (RuntimeException ex) {
            log.warn("Could not cancel stale Quartz triggers for question visual job {}", jobId, ex);
        }
    }

    private void cancelParentSafely(UUID parentId) {
        try {
            visualScheduler.cancelParent(parentId);
        } catch (RuntimeException ex) {
            log.warn("Could not cancel stale Quartz triggers for quiz generation job {}", parentId, ex);
        }
    }

    record VisualRequest(UUID visualJobId, String visualType, String questionContent,
                         String generationBrief, String altText) {}
    record VisualResponse(UUID visualJobId, String visualType, String sourceCode) {}
    static class UnsafeSourceException extends IllegalArgumentException {
        UnsafeSourceException(String message) { super(message); }
        UnsafeSourceException(String message, Throwable cause) { super(message, cause); }
    }
}
