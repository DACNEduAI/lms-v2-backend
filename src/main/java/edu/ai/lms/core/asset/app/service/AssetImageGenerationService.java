package edu.ai.lms.core.asset.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import edu.ai.lms.core.asset.adapter.out.persistence.AssetGenerationJobMapper;
import edu.ai.lms.core.asset.adapter.out.persistence.AssetGenerationJobRow;
import edu.ai.lms.core.asset.app.domain.Asset;
import edu.ai.lms.core.asset.app.domain.ImageAsset;
import edu.ai.lms.core.asset.app.exception.AssetValidationException;
import edu.ai.lms.core.asset.app.port.out.AssetRepository;
import edu.ai.lms.core.course.app.service.CourseTeacherAccess;
import edu.ai.lms.core.material.api.CourseAssetStorageApi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssetImageGenerationService {
    private static final String PENDING_NAME = "Generating image";
    private static final String PENDING_DESCRIPTION = "Review and describe this generated image before publication.";
    private final ImageAssetService images;
    private final AssetRepository assets;
    private final AssetGenerationJobMapper jobs;
    private final CourseTeacherAccess access;
    private final CourseAssetStorageApi storage;
    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;
    @Value("${asset.generation.webhook.url:http://localhost:8080/api/internal/assets/image-generations/callback}") private String webhookUrl;
    @Value("${asset.generation.webhook.secret:asset-generation}") private String webhookSecret;

    @Transactional
    public Accepted start(UUID courseId, String prompt) {
        access.requireCourseTeacher(courseId);
        if (prompt == null || prompt.isBlank() || prompt.length() > 5000) throw new AssetValidationException("Prompt must contain between 1 and 5000 characters");
        var upload = images.create(courseId, new ImageAssetService.CreateImage(PENDING_NAME, "generated-image.png", "image/png", PENDING_DESCRIPTION, prompt.trim(), List.of()));
        AssetGenerationJobRow job = new AssetGenerationJobRow();
        job.setId(UUID.randomUUID()); job.setAssetId(upload.image().asset().id()); job.setCourseId(courseId);
        job.setPrompt(prompt.trim()); job.setStatus("PENDING"); job.setAttempt(1); job.setParametersJson("{}");
        job.setGenerationMode("INITIAL"); job.setOutputStorageKey(upload.image().storageKey());
        jobs.insert(job);
        return new Accepted(job.getId(), upload.image().asset().id(), "PENDING");
    }

    @Transactional(readOnly = true)
    public View view(UUID courseId, UUID jobId) {
        access.requireCourseTeacher(courseId);
        AssetGenerationJobRow row = jobs.find(courseId, jobId);
        if (row == null) throw new AssetValidationException("Asset generation job not found");
        return new View(row.getId(), row.getAssetId(), row.getStatus(), row.getErrorMessage(), row.getErrorDetails());
    }

    @Async
    public void dispatch(UUID jobId, String uploadUrl) {
        if (jobs.markProcessing(jobId) != 1) return;
        AssetGenerationJobRow job = jobs.findForUpdate(jobId);
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jobId", job.getId()); request.put("courseId", job.getCourseId()); request.put("assetId", job.getAssetId()); request.put("prompt", job.getPrompt());
            request.put("uploadUrl", uploadUrl); request.put("webhookUrl", webhookUrl); request.put("webhookSecret", webhookSecret); request.put("attempt", job.getAttempt());
            if (job.getErrorDetails() != null && !job.getErrorDetails().isBlank()) request.put("previousRenderError", job.getErrorDetails());
            aiWebClient.post().uri("/assets/images/generate").bodyValue(request).retrieve().toBodilessEntity().block();
        } catch (Exception exception) { jobs.fail(jobId, "Unable to send image generation request", message(exception)); }
    }

    @Scheduled(fixedDelayString = "${asset.generation.dispatch-delay-ms:5000}")
    public void recoverPending() {
        for (AssetGenerationJobRow job : jobs.findPending()) {
            ImageAsset image = assets.findImage(job.getCourseId(), job.getAssetId()).orElse(null);
            if (image != null) dispatch(job.getId(), storage.createUploadLink(job.getOutputStorageKey(), "image/png"));
        }
    }

    @Transactional
    public Accepted regenerate(UUID courseId, UUID jobId) {
        access.requireCourseTeacher(courseId);
        AssetGenerationJobRow job = jobs.find(courseId, jobId);
        if (job == null) throw new AssetValidationException("Asset generation job not found");
        if (!"FAILED".equals(job.getStatus()) || jobs.retry(jobId) != 1) {
            throw new AssetValidationException("Only failed image generation jobs can be regenerated");
        }
        AssetGenerationJobRow retried = jobs.find(courseId, jobId);
        return new Accepted(retried.getId(), retried.getAssetId(), retried.getStatus());
    }

    @Transactional
    public Accepted regenerateAsset(UUID courseId, UUID assetId, String prompt) {
        access.requireCourseTeacher(courseId);
        if (prompt == null || prompt.isBlank() || prompt.length() > 5000) throw new AssetValidationException("Prompt must contain between 1 and 5000 characters");
        ImageAsset image = assets.findImage(courseId, assetId).orElseThrow(() -> new AssetValidationException("Image asset not found"));
        if (!image.aiGenerated()) throw new AssetValidationException("Only AI-generated images can be regenerated");
        if (image.asset().publicationStatus() != Asset.PublicationStatus.DRAFT) throw new AssetValidationException("Published images cannot be regenerated");
        AssetGenerationJobRow job = jobs.findForAsset(courseId, assetId);
        if (job == null) throw new AssetValidationException("Asset generation job not found");
        String outputStorageKey = "courses/" + courseId + "/assets/" + assetId + "/generations/" + UUID.randomUUID() + "/generated-image.png";
        if (jobs.restart(job.getId(), prompt.trim(), outputStorageKey) != 1) throw new AssetValidationException("Image generation is already in progress");
        Asset a = image.asset();
        assets.saveImage(new ImageAsset(new Asset(a.id(), a.courseId(), a.type(), a.name(), a.description(), prompt.trim(), a.tags(), a.publicationStatus(), a.uploadStatus(), a.createdAt(), LocalDateTime.now()), image.originalFileName(), image.contentType(), image.byteSize(), image.storageKey(), image.pendingReplacement(), image.aiGenerated()));
        AssetGenerationJobRow restarted = jobs.find(courseId, job.getId());
        return new Accepted(restarted.getId(), restarted.getAssetId(), restarted.getStatus());
    }

    @Transactional
    public String improveDescription(UUID courseId, UUID assetId) {
        access.requireCourseTeacher(courseId);
        ImageAsset image = assets.findImage(courseId, assetId).orElseThrow(() -> new AssetValidationException("Image asset not found"));
        if (!image.aiGenerated()) throw new AssetValidationException("Only AI-generated images can improve their description");
        if (image.asset().publicationStatus() != Asset.PublicationStatus.DRAFT) throw new AssetValidationException("Published images cannot improve their description");
        AssetGenerationJobRow job = jobs.findForAsset(courseId, assetId);
        if (job == null || job.getTikzSource() == null || job.getTikzSource().isBlank()) throw new AssetValidationException("No generated TikZ source is available for this image");
        DescriptionResponse response;
        try {
            response = aiWebClient.post().uri("/assets/images/improve-description").bodyValue(Map.of("tikzSource", job.getTikzSource())).retrieve().bodyToMono(DescriptionResponse.class).block();
        } catch (Exception exception) {
            throw new AssetValidationException("Unable to improve image description: " + message(exception));
        }
        String description = clean(response == null ? null : response.description(), null);
        if (description == null || description.length() > 5000) throw new AssetValidationException("AI returned an invalid image description");
        Asset a = image.asset();
        assets.saveImage(new ImageAsset(new Asset(a.id(), a.courseId(), a.type(), a.name(), description, a.prompt(), a.tags(), a.publicationStatus(), a.uploadStatus(), a.createdAt(), LocalDateTime.now()), image.originalFileName(), image.contentType(), image.byteSize(), image.storageKey(), image.pendingReplacement(), image.aiGenerated()));
        return description;
    }

    @Transactional
    public void complete(Completion completion) {
        AssetGenerationJobRow job = jobs.findForUpdate(completion.jobId());
        if (job == null || !job.getAssetId().equals(completion.assetId()) || job.getAttempt() != completion.attempt() || !"PROCESSING".equals(job.getStatus())) return;
        if ("FAILED".equals(completion.status())) {
            jobs.fail(job.getId(), shortMessage(completion.errorMessage()), detailMessage(completion.errorDetails()));
            return;
        }
        ImageAsset current = assets.findImage(job.getCourseId(), job.getAssetId()).orElseThrow();
        var object = storage.inspect(job.getOutputStorageKey());
        if (!object.exists() || object.contentLength() <= 0 || object.contentLength() > 10L * 1024 * 1024 || !"image/png".equalsIgnoreCase(object.contentType())) { jobs.fail(job.getId(), "Generated PNG upload is unavailable or invalid", "The generated PNG upload was missing, empty, too large, or had an unexpected content type."); return; }
        Asset a = current.asset();
        boolean regeneration = "REGENERATE".equals(job.getGenerationMode());
        Asset updated = new Asset(a.id(), a.courseId(), a.type(), regeneration ? a.name() : clean(completion.assetName(), PENDING_NAME), clean(completion.description(), PENDING_DESCRIPTION), a.prompt(), normalizeTags(completion.usageTags()), Asset.PublicationStatus.DRAFT, Asset.UploadStatus.UPLOADED, a.createdAt(), LocalDateTime.now());
        assets.saveImage(new ImageAsset(updated, "generated-image.png", "image/png", object.contentLength(), job.getOutputStorageKey(), null, current.aiGenerated()));
        if (!current.storageKey().equals(job.getOutputStorageKey())) { try { storage.delete(current.storageKey()); } catch (RuntimeException ignored) { } }
        job.setCallbackEventId(completion.eventId()); job.setTikzSource(completion.tikzSource()); job.setModel(completion.model()); job.setParametersJson(toJson(completion.parameters())); jobs.complete(job);
    }

    private String toJson(Map<String, Object> value) { try { return objectMapper.writeValueAsString(value == null ? Map.of() : value); } catch (Exception e) { return "{}"; } }
    private static String clean(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private static List<String> normalizeTags(List<String> tags) { return tags == null ? List.of() : tags.stream().filter(t -> t != null && !t.isBlank()).map(t -> t.trim().toLowerCase()).distinct().limit(30).toList(); }
    private static String shortMessage(String value) { return clean(value, "Image generation failed").substring(0, Math.min(1000, clean(value, "Image generation failed").length())); }
    private static String detailMessage(String value) { return clean(value, "No additional error details were provided.").substring(0, Math.min(5000, clean(value, "No additional error details were provided.").length())); }
    private static String message(Exception e) { String value = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); return value.substring(0, Math.min(5000, value.length())); }
    public record Accepted(UUID jobId, UUID assetId, String status) {}
    public record View(UUID jobId, UUID assetId, String status, String errorMessage, String errorDetails) {}
    public record DescriptionResponse(String description) {}
    public record Completion(UUID eventId, UUID jobId, UUID assetId, int attempt, String status, String errorMessage, String errorDetails, String assetName, String description, List<String> usageTags, String tikzSource, String model, Map<String, Object> parameters) {}
}
