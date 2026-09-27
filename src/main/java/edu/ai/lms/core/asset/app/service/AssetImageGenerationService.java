package edu.ai.lms.core.asset.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
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
        jobs.insert(job);
        return new Accepted(job.getId(), upload.image().asset().id(), "PENDING");
    }

    @Transactional(readOnly = true)
    public View view(UUID courseId, UUID jobId) {
        access.requireCourseTeacher(courseId);
        AssetGenerationJobRow row = jobs.find(courseId, jobId);
        if (row == null) throw new AssetValidationException("Asset generation job not found");
        return new View(row.getId(), row.getAssetId(), row.getStatus(), row.getErrorMessage());
    }

    @Async
    public void dispatch(UUID jobId, String uploadUrl) {
        if (jobs.markProcessing(jobId) != 1) return;
        AssetGenerationJobRow job = jobs.findForUpdate(jobId);
        try {
            aiWebClient.post().uri("/assets/images/generate").bodyValue(Map.of(
                    "jobId", job.getId(), "courseId", job.getCourseId(), "assetId", job.getAssetId(), "prompt", job.getPrompt(),
                    "uploadUrl", uploadUrl, "webhookUrl", webhookUrl,
                    "webhookSecret", webhookSecret, "attempt", job.getAttempt())).retrieve().toBodilessEntity().block();
        } catch (Exception exception) { jobs.fail(jobId, message(exception)); }
    }

    @Scheduled(fixedDelayString = "${asset.generation.dispatch-delay-ms:5000}")
    public void recoverPending() {
        for (AssetGenerationJobRow job : jobs.findPending()) {
            ImageAsset image = assets.findImage(job.getCourseId(), job.getAssetId()).orElse(null);
            if (image != null) dispatch(job.getId(), storage.createUploadLink(image.storageKey(), "image/png"));
        }
    }

    @Transactional
    public void complete(Completion completion) {
        AssetGenerationJobRow job = jobs.findForUpdate(completion.jobId());
        if (job == null || !job.getAssetId().equals(completion.assetId()) || job.getAttempt() != completion.attempt() || !"PROCESSING".equals(job.getStatus())) return;
        if ("FAILED".equals(completion.status())) { jobs.fail(job.getId(), completion.errorMessage() == null ? "Image generation failed" : completion.errorMessage().substring(0, Math.min(1000, completion.errorMessage().length()))); return; }
        ImageAsset current = assets.findImage(job.getCourseId(), job.getAssetId()).orElseThrow();
        var object = storage.inspect(current.storageKey());
        if (!object.exists() || object.contentLength() <= 0 || object.contentLength() > 10L * 1024 * 1024 || !"image/png".equalsIgnoreCase(object.contentType())) { jobs.fail(job.getId(), "Generated PNG upload is unavailable or invalid"); return; }
        Asset a = current.asset();
        Asset updated = new Asset(a.id(), a.courseId(), a.type(), clean(completion.assetName(), PENDING_NAME), clean(completion.description(), PENDING_DESCRIPTION), a.prompt(), normalizeTags(completion.usageTags()), Asset.PublicationStatus.DRAFT, Asset.UploadStatus.UPLOADED, a.createdAt(), LocalDateTime.now());
        assets.saveImage(new ImageAsset(updated, "generated-image.png", "image/png", object.contentLength(), current.storageKey(), null));
        job.setCallbackEventId(completion.eventId()); job.setTikzSource(completion.tikzSource()); job.setModel(completion.model()); job.setParametersJson(toJson(completion.parameters())); jobs.complete(job);
    }

    private String toJson(Map<String, Object> value) { try { return objectMapper.writeValueAsString(value == null ? Map.of() : value); } catch (Exception e) { return "{}"; } }
    private static String clean(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private static List<String> normalizeTags(List<String> tags) { return tags == null ? List.of() : tags.stream().filter(t -> t != null && !t.isBlank()).map(t -> t.trim().toLowerCase()).distinct().limit(30).toList(); }
    private static String message(Exception e) { String value = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); return value.substring(0, Math.min(1000, value.length())); }
    public record Accepted(UUID jobId, UUID assetId, String status) {}
    public record View(UUID jobId, UUID assetId, String status, String errorMessage) {}
    public record Completion(UUID eventId, UUID jobId, UUID assetId, int attempt, String status, String errorMessage, String assetName, String description, List<String> usageTags, String tikzSource, String model, Map<String, Object> parameters) {}
}
