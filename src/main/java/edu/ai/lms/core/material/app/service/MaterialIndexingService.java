package edu.ai.lms.core.material.app.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import edu.ai.lms.config.CorrelationIdFilter;
import edu.ai.lms.core.material.adapter.in.response.ConfirmUploadResponse;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.domain.DocumentIndex;
import edu.ai.lms.core.material.app.domain.MaterialIndexJob;
import edu.ai.lms.core.material.app.exception.MaterialNotFoundException;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.MaterialIndexJobPort;
import edu.ai.lms.core.material.app.port.out.persistence.ModifyMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.DocumentIndexPort;
import edu.ai.lms.core.material.app.port.out.scheduler.ScheduleMaterialIndexJobPort;
import edu.ai.lms.core.material.app.port.out.storage.CreateDownloadLinkPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialIndexingService {
    private final MaterialIndexJobPort jobPort;
    private final ScheduleMaterialIndexJobPort schedulerPort;
    private final GetMaterialPort materialPort;
    private final ModifyMaterialPort modifyMaterialPort;
    private final CreateDownloadLinkPort downloadLinkPort;
    private final WebClient aiWebClient;
    private final DocumentIndexPort documentIndexPort;

    @Value("${material.indexing.max-attempts:3}")
    private int maxAttempts;
    @Value("${material.indexing.timeout-minutes:15}")
    private long timeoutMinutes;
    @Value("${material.indexing.profile:lms-index-v1}")
    private String indexProfile;
    @Value("${material.indexing.bucket:lms-materials}")
    private String bucketName;

    public MaterialIndexJob queueIngest(Material material) {
        MaterialIndexJob job = MaterialIndexJob.builder()
                .courseId(material.getCourseId())
                .materialId(material.getId())
                .operation(MaterialIndexJob.Operation.INGEST)
                .revision(material.getIngestionRevision())
                .fileKey(material.getFileKey())
                .status(MaterialIndexJob.Status.PENDING)
                .attemptCount(0)
                .nextAttemptAt(Instant.now())
                .build();
        MaterialIndexJob saved = jobPort.save(job);
        log.info("event=material.index.queued job_id={} course_id={} material_id={} operation={} revision={}",
                saved.getId(), saved.getCourseId(), saved.getMaterialId(), saved.getOperation(), saved.getRevision());
        scheduleAfterCommit(saved);
        return saved;
    }

    public MaterialIndexJob queueDelete(UUID courseId, UUID materialId, String fileKey) {
        MaterialIndexJob job = MaterialIndexJob.builder()
                .courseId(courseId)
                .materialId(materialId)
                .operation(MaterialIndexJob.Operation.DELETE)
                .fileKey(fileKey)
                .status(MaterialIndexJob.Status.PENDING)
                .attemptCount(0)
                .nextAttemptAt(Instant.now())
                .build();
        MaterialIndexJob saved = jobPort.save(job);
        log.info("event=material.index.queued job_id={} course_id={} material_id={} operation={}",
                saved.getId(), saved.getCourseId(), saved.getMaterialId(), saved.getOperation());
        scheduleAfterCommit(saved);
        return saved;
    }

    @Transactional
    public ConfirmUploadResponse retry(UUID courseId, UUID materialId) {
        Material material = materialPort.findByIdAndCourseId(materialId, courseId)
                .orElseThrow(() -> new MaterialNotFoundException("Material not found: " + materialId));
        if (material.getStatus() != Material.MaterialStatus.UPLOADED) {
            throw new IllegalStateException("Material upload is not complete");
        }
        if (material.getType() != Material.MaterialType.PDF) {
            throw new IllegalStateException("Only PDF materials can be indexed");
        }
        material.setIngestionStatus(Material.IngestionStatus.PENDING);
        material.setIngestionError(null);
        material.setIngestedAt(null);
        if (material.getIngestionRevision() == null) {
            material.setIngestionRevision(UUID.randomUUID());
        }
        Material updated = modifyMaterialPort.update(material);
        queueIngest(updated);
        return ConfirmUploadResponse.builder()
                .materialId(updated.getId()).courseId(updated.getCourseId())
                .fileKey(updated.getFileKey()).status(updated.getStatus().name())
                .ingestionStatus(updated.getIngestionStatus().name())
                .uploaded(true).message("Material indexing has been queued.").build();
    }

    public void process(UUID jobId) {
        MaterialIndexJob initial = jobPort.findById(jobId).orElse(null);
        if (initial == null) {
            log.warn("event=material.index.skipped job_id={} reason=not_found", jobId);
            return;
        }
        if (isTerminal(initial.getStatus())) {
            log.info("event=material.index.skipped job_id={} status={} reason=terminal", jobId, initial.getStatus());
            return;
        }
        if (!jobPort.claim(jobId, Instant.now().plus(Duration.ofMinutes(timeoutMinutes)))) {
            log.info("event=material.index.skipped job_id={} reason=not_claimed", jobId);
            return;
        }
        MaterialIndexJob job = jobPort.findById(jobId).orElseThrow();
        long startedAt = System.nanoTime();
        log.info("event=material.index.started job_id={} course_id={} material_id={} operation={} attempt={}",
                job.getId(), job.getCourseId(), job.getMaterialId(), job.getOperation(), job.getAttemptCount());
        try {
            if (job.getOperation() == MaterialIndexJob.Operation.DELETE) {
                aiWebClient.delete()
                        .uri(builder -> builder.path("/api/ingest/{courseId}/{materialId}")
                                .queryParam("document_key", documentKey(job.getFileKey()))
                                .build(job.getCourseId(), job.getMaterialId()))
                        .header(CorrelationIdFilter.HEADER_NAME, job.getId().toString())
                        .retrieve().toBodilessEntity().block(Duration.ofMinutes(timeoutMinutes));
                documentIndexPort.deleteByDocument(bucketName, job.getFileKey());
            } else {
                ingest(job);
            }
            job.setStatus(MaterialIndexJob.Status.COMPLETED);
            job.setLeaseUntil(null);
            job.setErrorMessage(null);
            job.setCompletedAt(Instant.now());
            jobPort.save(job);
            log.info("event=material.index.completed job_id={} course_id={} material_id={} operation={} duration_ms={}",
                    job.getId(), job.getCourseId(), job.getMaterialId(), job.getOperation(), elapsedMillis(startedAt));
        } catch (Exception ex) {
            log.error("event=material.index.attempt_failed job_id={} course_id={} material_id={} operation={} duration_ms={} error_type={}",
                    job.getId(), job.getCourseId(), job.getMaterialId(), job.getOperation(), elapsedMillis(startedAt),
                    ex.getClass().getSimpleName(), ex);
            retryOrFail(job, safeMessage(ex));
        }
    }

    private void ingest(MaterialIndexJob job) {
        Material material = materialPort.findByIdAndCourseId(job.getMaterialId(), job.getCourseId())
                .orElse(null);
        if (material == null || !job.getRevision().equals(material.getIngestionRevision())) {
            job.setStatus(MaterialIndexJob.Status.CANCELLED);
            job.setLeaseUntil(null);
            job.setCompletedAt(Instant.now());
            jobPort.save(job);
            throw new StaleMaterialRevisionException();
        }
        material.setIngestionStatus(Material.IngestionStatus.PROCESSING);
        material.setIngestionError(null);
        modifyMaterialPort.update(material);

        DocumentIndex existing = documentIndexPort.findReady(bucketName, material.getFileKey(), indexProfile)
                .orElse(null);
        JsonNode response = callAiIngest(job, material, existing == null ? null : existing.getSourceHash(), null);
        String status = response == null ? "" : response.path("status").asText();
        if (!"skipped".equals(status) && !"success".equals(status) && !"missing".equals(status)) {
            throw new IllegalStateException("AI ingestion did not complete: " + status);
        }
        if ("missing".equals(status)) {
            String fileUrl = downloadLinkPort.createDownloadLink(material.getFileKey(), null);
            response = callAiIngest(job, material, null, fileUrl);
            status = response == null ? "" : response.path("status").asText();
        }
        if (!"success".equals(status) && !"skipped".equals(status)) {
            throw new IllegalStateException("AI ingestion did not complete: " + status);
        }

        String sourceHash = response.path("source_hash").asText();
        if (sourceHash.isBlank()) {
            throw new IllegalStateException("AI ingestion completed without source_hash");
        }
        documentIndexPort.save(DocumentIndex.builder()
                .bucketName(bucketName).fileKey(material.getFileKey()).indexProfile(indexProfile)
                .sourceHash(sourceHash).collectionName("eduai_chunks")
                .status(DocumentIndex.Status.READY)
                .chunkCount(response.path("chunks_created").asInt(0))
                .indexedAt(LocalDateTime.now())
                .build());

        Material current = materialPort.findByIdAndCourseId(job.getMaterialId(), job.getCourseId())
                .orElseThrow();
        if (!job.getRevision().equals(current.getIngestionRevision())) {
            throw new StaleMaterialRevisionException();
        }
        current.setIngestionStatus(Material.IngestionStatus.READY);
        current.setIngestionError(null);
        current.setIngestedAt(LocalDateTime.now());
        modifyMaterialPort.update(current);
    }

    private JsonNode callAiIngest(
            MaterialIndexJob job, Material material, String sourceHash, String fileUrl) {
        return aiWebClient.post().uri("/api/ingest")
                .header(CorrelationIdFilter.HEADER_NAME, job.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AiIngestRequest(
                        material.getCourseId().toString(), material.getId().toString(), fileUrl,
                        documentKey(material.getFileKey()), indexProfile, sourceHash))
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("No response body")
                        .map(body -> new IllegalStateException(
                                "AI ingestion request failed (" + clientResponse.statusCode() + "): " + body)))
                .bodyToMono(JsonNode.class).block(Duration.ofMinutes(timeoutMinutes));
    }

    private void retryOrFail(MaterialIndexJob job, String message) {
        if (job.getStatus() == MaterialIndexJob.Status.CANCELLED) return;
        boolean exhausted = job.getAttemptCount() >= maxAttempts;
        job.setStatus(exhausted ? MaterialIndexJob.Status.FAILED : MaterialIndexJob.Status.PENDING);
        job.setLeaseUntil(null);
        job.setErrorMessage(message);
        job.setNextAttemptAt(exhausted ? null : Instant.now().plusSeconds(backoffSeconds(job.getAttemptCount())));
        if (exhausted) job.setCompletedAt(Instant.now());
        MaterialIndexJob saved = jobPort.save(job);
        log.warn("event=material.index.retry_scheduled job_id={} course_id={} material_id={} status={} attempt={} next_attempt_at={}",
                job.getId(), job.getCourseId(), job.getMaterialId(), saved.getStatus(), saved.getAttemptCount(),
                saved.getNextAttemptAt());

        materialPort.findByIdAndCourseId(job.getMaterialId(), job.getCourseId())
                .filter(material -> job.getRevision() != null && job.getRevision().equals(material.getIngestionRevision()))
                .ifPresent(material -> {
                    material.setIngestionStatus(exhausted
                            ? Material.IngestionStatus.FAILED : Material.IngestionStatus.PENDING);
                    material.setIngestionError(message);
                    modifyMaterialPort.update(material);
                });
        if (!exhausted) schedulerPort.schedule(saved.getId(), saved.getNextAttemptAt());
    }

    private long backoffSeconds(int attemptCount) {
        return Math.min(300, 10L * (1L << Math.min(5, Math.max(0, attemptCount - 1))));
    }

    private void scheduleAfterCommit(MaterialIndexJob job) {
        Runnable schedule = () -> {
            try {
                schedulerPort.schedule(job.getId(), job.getNextAttemptAt());
            } catch (RuntimeException ex) {
                log.warn("Material index job {} was persisted but could not be scheduled; recovery will retry it",
                        job.getId(), ex);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { schedule.run(); }
            });
        } else {
            schedule.run();
        }
    }

    private boolean isTerminal(MaterialIndexJob.Status status) {
        return status == MaterialIndexJob.Status.COMPLETED || status == MaterialIndexJob.Status.FAILED
                || status == MaterialIndexJob.Status.CANCELLED;
    }

    private String safeMessage(Exception ex) {
        String value = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return value.substring(0, Math.min(1000, value.length()));
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private record AiIngestRequest(
            @JsonProperty("course_id") String courseId,
            @JsonProperty("material_id") String materialId,
            @JsonProperty("file_url") String fileUrl,
            @JsonProperty("document_key") String documentKey,
            @JsonProperty("index_profile") String indexProfile,
            @JsonProperty("source_hash") String sourceHash) {}

    private String documentKey(String fileKey) {
        return bucketName + "/" + fileKey;
    }

    private static class StaleMaterialRevisionException extends RuntimeException {
        StaleMaterialRevisionException() { super("Material revision changed while indexing"); }
    }
}
