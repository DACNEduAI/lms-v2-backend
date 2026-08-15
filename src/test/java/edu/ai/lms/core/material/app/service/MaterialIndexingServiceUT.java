package edu.ai.lms.core.material.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.domain.DocumentIndex;
import edu.ai.lms.core.material.app.domain.MaterialIndexJob;
import edu.ai.lms.core.material.app.port.out.persistence.DocumentIndexPort;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.MaterialIndexJobPort;
import edu.ai.lms.core.material.app.port.out.persistence.ModifyMaterialPort;
import edu.ai.lms.core.material.app.port.out.scheduler.ScheduleMaterialIndexJobPort;
import edu.ai.lms.core.material.app.port.out.storage.CreateDownloadLinkPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialIndexingServiceUT {
    @Mock MaterialIndexJobPort jobPort;
    @Mock ScheduleMaterialIndexJobPort schedulerPort;
    @Mock GetMaterialPort materialPort;
    @Mock ModifyMaterialPort modifyMaterialPort;
    @Mock CreateDownloadLinkPort downloadLinkPort;
    @Mock DocumentIndexPort documentIndexPort;

    private MaterialIndexingService service;

    @BeforeEach
    void setUp() {
        service = createService(request -> jsonResponse("{\"status\":\"success\",\"source_hash\":\"sha-default\",\"chunks_created\":3}"));
    }

    @Test
    void queueIngestPreservesCourseMaterialAndRevision() {
        Material material = readyToQueue();
        when(jobPort.save(any())).thenAnswer(invocation -> {
            MaterialIndexJob job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        MaterialIndexJob job = service.queueIngest(material);

        assertEquals(material.getCourseId(), job.getCourseId());
        assertEquals(material.getId(), job.getMaterialId());
        assertEquals(material.getIngestionRevision(), job.getRevision());
        assertEquals(MaterialIndexJob.Operation.INGEST, job.getOperation());
        verify(schedulerPort).schedule(job.getId(), job.getNextAttemptAt());
    }

    @Test
    void queueIngestUsesAnAbsoluteInstantWhenJvmRunsInHoChiMinhTime() {
        TimeZone original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        try {
            Material material = readyToQueue();
            when(jobPort.save(any())).thenAnswer(invocation -> {
                MaterialIndexJob job = invocation.getArgument(0);
                job.setId(UUID.randomUUID());
                return job;
            });
            Instant before = Instant.now();

            MaterialIndexJob job = service.queueIngest(material);

            Instant after = Instant.now();
            assertFalse(job.getNextAttemptAt().isBefore(before));
            assertFalse(job.getNextAttemptAt().isAfter(after));
            assertTrue(TimeZone.getDefault().hasSameRules(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void queueIngestSchedulesOnlyAfterTransactionCommit() {
        Material material = readyToQueue();
        when(jobPort.save(any())).thenAnswer(invocation -> {
            MaterialIndexJob job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            MaterialIndexJob job = service.queueIngest(material);

            verifyNoInteractions(schedulerPort);
            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
            verify(schedulerPort).schedule(job.getId(), job.getNextAttemptAt());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void missingCloudIndexDownloadsAndRegistersMatchingRevision() {
        AtomicInteger calls = new AtomicInteger();
        service = createService(request -> calls.getAndIncrement() == 0
                ? jsonResponse("{\"status\":\"missing\"}")
                : jsonResponse("{\"status\":\"success\",\"source_hash\":\"sha-new\",\"chunks_created\":4}"));
        Material material = readyToQueue();
        MaterialIndexJob job = MaterialIndexJob.builder()
                .id(UUID.randomUUID()).courseId(material.getCourseId()).materialId(material.getId())
                .revision(material.getIngestionRevision()).operation(MaterialIndexJob.Operation.INGEST)
                .status(MaterialIndexJob.Status.PROCESSING).attemptCount(1).build();
        when(jobPort.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobPort.claim(eq(job.getId()), any())).thenReturn(true);
        when(materialPort.findByIdAndCourseId(material.getId(), material.getCourseId()))
                .thenReturn(Optional.of(material));
        when(modifyMaterialPort.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(downloadLinkPort.createDownloadLink(material.getFileKey(), null)).thenReturn("https://files/notes.pdf");
        when(jobPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.process(job.getId());

        assertEquals(Material.IngestionStatus.READY, material.getIngestionStatus());
        verify(downloadLinkPort).createDownloadLink(material.getFileKey(), null);
        verify(documentIndexPort).save(argThat(index ->
                "sha-new".equals(index.getSourceHash()) && index.getChunkCount() == 4));
        verify(modifyMaterialPort, atLeast(2)).update(material);
    }

    @Test
    void existingReadyCloudIndexSkipsDownloadAndMarksMaterialReady() {
        Material material = readyToQueue();
        MaterialIndexJob job = MaterialIndexJob.builder()
                .id(UUID.randomUUID()).courseId(material.getCourseId()).materialId(material.getId())
                .revision(material.getIngestionRevision()).operation(MaterialIndexJob.Operation.INGEST)
                .status(MaterialIndexJob.Status.PROCESSING).attemptCount(1).build();
        when(jobPort.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobPort.claim(eq(job.getId()), any())).thenReturn(true);
        when(materialPort.findByIdAndCourseId(material.getId(), material.getCourseId()))
                .thenReturn(Optional.of(material));
        when(modifyMaterialPort.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentIndexPort.findReady("lms-materials", material.getFileKey(), "lms-index-v1"))
                .thenReturn(Optional.of(DocumentIndex.builder().bucketName("lms-materials")
                        .fileKey(material.getFileKey()).indexProfile("lms-index-v1")
                        .sourceHash("sha-existing").status(DocumentIndex.Status.READY).build()));
        service = createService(request -> jsonResponse(
                "{\"status\":\"skipped\",\"source_hash\":\"sha-existing\",\"chunks_created\":4}"));

        service.process(job.getId());

        assertEquals(Material.IngestionStatus.READY, material.getIngestionStatus());
        verifyNoInteractions(downloadLinkPort);
        verify(documentIndexPort).save(argThat(index ->
                "sha-existing".equals(index.getSourceHash()) && index.getChunkCount() == 4));
    }

    @Test
    void deleteProcessingRemovesCloudRegistryAfterAiDeletion() {
        UUID courseId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        String fileKey = "courses/course/notes.pdf";
        MaterialIndexJob job = MaterialIndexJob.builder()
                .id(UUID.randomUUID()).courseId(courseId).materialId(materialId).fileKey(fileKey)
                .operation(MaterialIndexJob.Operation.DELETE).status(MaterialIndexJob.Status.PENDING)
                .attemptCount(1).build();
        when(jobPort.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobPort.claim(eq(job.getId()), any())).thenReturn(true);
        when(jobPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.process(job.getId());

        assertEquals(MaterialIndexJob.Status.COMPLETED, job.getStatus());
        verify(documentIndexPort).deleteByDocument("lms-materials", fileKey);
    }

    @Test
    void staleJobCannotMarkNewerMaterialRevisionReady() {
        Material material = readyToQueue();
        UUID staleRevision = UUID.randomUUID();
        MaterialIndexJob job = MaterialIndexJob.builder()
                .id(UUID.randomUUID()).courseId(material.getCourseId()).materialId(material.getId())
                .revision(staleRevision).operation(MaterialIndexJob.Operation.INGEST)
                .status(MaterialIndexJob.Status.PENDING).attemptCount(1).build();
        when(jobPort.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobPort.claim(eq(job.getId()), any())).thenReturn(true);
        when(materialPort.findByIdAndCourseId(material.getId(), material.getCourseId()))
                .thenReturn(Optional.of(material));
        when(jobPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.process(job.getId());

        assertEquals(MaterialIndexJob.Status.CANCELLED, job.getStatus());
        assertEquals(Material.IngestionStatus.PENDING, material.getIngestionStatus());
        verifyNoInteractions(downloadLinkPort);
        verifyNoInteractions(modifyMaterialPort);
    }

    @Test
    void retryCreatesAFreshDownloadUrlForEveryAttempt() {
        AtomicInteger calls = new AtomicInteger();
        service = createService(request -> switch (calls.getAndIncrement()) {
            case 0, 2 -> jsonResponse("{\"status\":\"missing\"}");
            case 1 -> Mono.just(org.springframework.web.reactive.function.client.ClientResponse
                    .create(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE).build());
            default -> jsonResponse("{\"status\":\"success\",\"source_hash\":\"sha-retry\",\"chunks_created\":2}");
        });

        Material material = readyToQueue();
        MaterialIndexJob job = MaterialIndexJob.builder()
                .id(UUID.randomUUID()).courseId(material.getCourseId()).materialId(material.getId())
                .revision(material.getIngestionRevision()).operation(MaterialIndexJob.Operation.INGEST)
                .status(MaterialIndexJob.Status.PENDING).attemptCount(1).build();
        when(jobPort.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobPort.claim(eq(job.getId()), any())).thenReturn(true);
        when(materialPort.findByIdAndCourseId(material.getId(), material.getCourseId()))
                .thenReturn(Optional.of(material));
        when(modifyMaterialPort.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(downloadLinkPort.createDownloadLink(material.getFileKey(), null))
                .thenReturn("https://files/attempt-1.pdf", "https://files/attempt-2.pdf");
        when(jobPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.process(job.getId());
        service.process(job.getId());

        assertEquals(Material.IngestionStatus.READY, material.getIngestionStatus());
        verify(downloadLinkPort, times(2)).createDownloadLink(material.getFileKey(), null);
    }

    private Material readyToQueue() {
        return Material.builder()
                .id(UUID.randomUUID()).courseId(UUID.randomUUID()).type(Material.MaterialType.PDF)
                .fileKey("courses/course/notes.pdf").status(Material.MaterialStatus.UPLOADED)
                .ingestionStatus(Material.IngestionStatus.PENDING).ingestionRevision(UUID.randomUUID())
                .createdAt(LocalDateTime.now()).build();
    }

    private MaterialIndexingService createService(ExchangeFunction exchange) {
        MaterialIndexingService created = new MaterialIndexingService(jobPort, schedulerPort, materialPort,
                modifyMaterialPort, downloadLinkPort, WebClient.builder().exchangeFunction(exchange).build(),
                documentIndexPort);
        ReflectionTestUtils.setField(created, "maxAttempts", 3);
        ReflectionTestUtils.setField(created, "timeoutMinutes", 1L);
        ReflectionTestUtils.setField(created, "indexProfile", "lms-index-v1");
        ReflectionTestUtils.setField(created, "bucketName", "lms-materials");
        return created;
    }

    private Mono<org.springframework.web.reactive.function.client.ClientResponse> jsonResponse(String body) {
        return Mono.just(org.springframework.web.reactive.function.client.ClientResponse
                .create(org.springframework.http.HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(body).build());
    }
}
