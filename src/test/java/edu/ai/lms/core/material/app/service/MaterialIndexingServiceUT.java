package edu.ai.lms.core.material.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.domain.MaterialIndexJob;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialIndexingServiceUT {
    @Mock MaterialIndexJobPort jobPort;
    @Mock ScheduleMaterialIndexJobPort schedulerPort;
    @Mock GetMaterialPort materialPort;
    @Mock ModifyMaterialPort modifyMaterialPort;
    @Mock CreateDownloadLinkPort downloadLinkPort;

    private MaterialIndexingService service;

    @BeforeEach
    void setUp() {
        ExchangeFunction exchange = request -> Mono.just(org.springframework.web.reactive.function.client.ClientResponse
                .create(org.springframework.http.HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{\"status\":\"success\"}").build());
        service = new MaterialIndexingService(jobPort, schedulerPort, materialPort, modifyMaterialPort,
                downloadLinkPort, WebClient.builder().exchangeFunction(exchange).build());
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        ReflectionTestUtils.setField(service, "timeoutMinutes", 1L);
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
    void successfulProcessingMarksMatchingRevisionReady() {
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
        verify(modifyMaterialPort, atLeast(2)).update(material);
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
        ExchangeFunction exchange = request -> {
            if (calls.getAndIncrement() == 0) {
                return Mono.just(org.springframework.web.reactive.function.client.ClientResponse
                        .create(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE).build());
            }
            return Mono.just(org.springframework.web.reactive.function.client.ClientResponse
                    .create(org.springframework.http.HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body("{\"status\":\"success\"}").build());
        };
        service = new MaterialIndexingService(jobPort, schedulerPort, materialPort, modifyMaterialPort,
                downloadLinkPort, WebClient.builder().exchangeFunction(exchange).build());
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        ReflectionTestUtils.setField(service, "timeoutMinutes", 1L);

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
}
