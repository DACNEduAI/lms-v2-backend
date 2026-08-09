package edu.ai.lms.core.quiz.app.service.genquiz;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionImagePublicUrlPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.QuestionVisualJobPort;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuestionVisualJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.UploadQuestionImagePort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionVisualGenerationServiceUT {
    @Mock private QuestionVisualJobPort visualJobs;
    @Mock private LoadQuizGenerationJobPort loadParent;
    @Mock private SaveQuizGenerationJobPort saveParent;
    @Mock private UploadQuestionImagePort uploadImage;
    @Mock private LoadQuestionImagePublicUrlPort publicUrl;
    @Mock private WebClient aiWebClient;
    @Mock private SseQuizGenerationService sse;
    @Mock private LoadSignedInUserPort signedInUser;
    @Mock private ScheduleQuestionVisualJobPort visualScheduler;
    @Mock private QuizGenerationTimingLogger timingLogger;
    @Mock private ThreadPoolTaskExecutor executor;

    private QuestionVisualGenerationService service;

    @BeforeEach
    void setUp() {
        service = spy(new QuestionVisualGenerationService(
                visualJobs, loadParent, saveParent, uploadImage, publicUrl, aiWebClient, sse,
                signedInUser, visualScheduler, timingLogger, 3, 20, 120, 30, executor));
        lenient().when(visualJobs.claimAvailable(anyInt(), any(LocalDateTime.class))).thenReturn(List.of());
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void defersDispatchUntilTransactionCommit() {
        TransactionSynchronizationManager.initSynchronization();
        UUID parentId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        service.requestDispatchAfterCommit(parentId, createdAt);

        verify(visualScheduler, never()).scheduleImmediate(any(), any());
        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());
        verify(visualScheduler).scheduleImmediate(eq(parentId), any(LocalDateTime.class));
        verify(visualScheduler).scheduleDeadline(parentId, createdAt.plusMinutes(20));
    }

    @Test
    void claimsOnlyConfiguredConcurrencyAndDoesNotBuildAnExecutorBacklog() {
        List<QuestionVisualJobPort.WorkItem> firstBatch = List.of(workItem(), workItem(), workItem());
        when(visualJobs.claimAvailable(eq(3), any(LocalDateTime.class))).thenReturn(firstBatch);

        service.requestDispatch();
        service.requestDispatch();

        verify(executor, times(3)).execute(any(Runnable.class));
        verify(visualJobs).claimAvailable(eq(3), any(LocalDateTime.class));
    }

    @Test
    void completedWorkerImmediatelyRefillsItsReleasedSlot() {
        QuestionVisualJobPort.WorkItem first = workItem();
        QuestionVisualJobPort.WorkItem second = workItem();
        when(visualJobs.claimAvailable(anyInt(), any(LocalDateTime.class)))
                .thenReturn(List.of(first), List.of(second), List.of());
        doNothing().when(service).process(any(QuestionVisualJobPort.WorkItem.class));
        ArgumentCaptor<Runnable> firstTask = ArgumentCaptor.forClass(Runnable.class);

        service.requestDispatch();
        verify(executor).execute(firstTask.capture());
        firstTask.getValue().run();

        ArgumentCaptor<Runnable> allTasks = ArgumentCaptor.forClass(Runnable.class);
        verify(executor, times(2)).execute(allTasks.capture());
        allTasks.getAllValues().get(1).run();
        verify(service).process(first);
        verify(service).process(second);
    }

    @Test
    void rejectedExecutorSubmissionReturnsJobToDurableQueueAndSchedulesWakeup() {
        QuestionVisualJobPort.WorkItem work = workItem();
        when(visualJobs.claimAvailable(eq(3), any(LocalDateTime.class))).thenReturn(List.of(work));
        when(visualJobs.retry(eq(work.id()), eq("WORKER_BUSY"), eq("Visual worker is busy"),
                any(LocalDateTime.class))).thenReturn(true);
        doThrow(new RejectedExecutionException("busy")).when(executor).execute(any(Runnable.class));

        service.requestDispatch();

        verify(visualJobs).retry(eq(work.id()), eq("WORKER_BUSY"), eq("Visual worker is busy"),
                any(LocalDateTime.class));
        verify(visualScheduler).scheduleRetry(eq(work.id()), any(LocalDateTime.class));
    }

    @Test
    void transientVisualFailurePersistsBackoffAndSchedulesExactWakeup() {
        QuestionVisualJobPort.WorkItem work = workItem();
        doThrow(new RuntimeException("AI unavailable")).when(service).requestVisual(work);
        when(visualJobs.retry(eq(work.id()), eq("VISUAL_SERVICE_ERROR"),
                eq("Visual generation will be retried"), any(LocalDateTime.class))).thenReturn(true);

        service.process(work);

        verify(visualJobs).retry(eq(work.id()), eq("VISUAL_SERVICE_ERROR"),
                eq("Visual generation will be retried"), any(LocalDateTime.class));
        verify(visualScheduler).scheduleRetry(eq(work.id()), any(LocalDateTime.class));
        verify(timingLogger).visualRetry(work, "VISUAL_SERVICE_ERROR", 1);
    }

    @Test
    void manualRetryDispatchesImmediatelyAfterResettingDurableJob() {
        UUID teacherId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        when(signedInUser.getAuthenticatedUser()).thenReturn(Optional.of(
                CurrentActor.builder().id(teacherId).role(Roles.TEACHER).build()));
        UUID jobId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        LocalDateTime parentCreatedAt = LocalDateTime.now();
        when(visualJobs.retryFailed(quizId, questionId, teacherId)).thenReturn(Optional.of(
                new QuestionVisualJobPort.RetryItem(jobId, parentId, parentCreatedAt)));

        service.retryFailed(quizId, questionId);

        verify(visualScheduler).scheduleRetry(eq(jobId), any(LocalDateTime.class));
        verify(visualScheduler).scheduleDeadline(parentId, parentCreatedAt.plusMinutes(20));
    }

    @Test
    void startupReconciliationCreatesOnlyOneShotSchedulesForActiveRows() {
        UUID parentId = UUID.randomUUID();
        UUID pendingId = UUID.randomUUID();
        UUID processingId = UUID.randomUUID();
        LocalDateTime parentCreatedAt = LocalDateTime.now().minusMinutes(2);
        when(visualJobs.findActiveForScheduling()).thenReturn(List.of(
                new QuestionVisualJobPort.RecoveryItem(pendingId, parentId, "PENDING",
                        LocalDateTime.now().plusSeconds(3), null, parentCreatedAt),
                new QuestionVisualJobPort.RecoveryItem(processingId, parentId, "PROCESSING",
                        null, LocalDateTime.now().plusSeconds(30), parentCreatedAt)));

        service.reconcileSchedules();

        verify(visualScheduler).scheduleDeadline(parentId, parentCreatedAt.plusMinutes(20));
        verify(visualScheduler).scheduleRetry(eq(pendingId), any(LocalDateTime.class));
        verify(visualScheduler).scheduleLeaseRecovery(eq(processingId), any(LocalDateTime.class));
    }

    @Test
    void deadlineFailsActiveChildrenAndCompletesParentWithWarning() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(1);
        QuestionVisualJobPort.Summary summary = new QuestionVisualJobPort.Summary(2, 1, 1, 0, startedAt);
        QuestionVisualJobPort.ExpiredItem expired = new QuestionVisualJobPort.ExpiredItem(
                childId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "AUTO", 1, startedAt);
        when(visualJobs.expireParent(eq(parentId), eq("VISUAL_DEADLINE_EXCEEDED"), any()))
                .thenReturn(new QuestionVisualJobPort.DeadlineResult(true, List.of(expired), summary));
        QuizGenerationJob parent = QuizGenerationJob.builder().id(parentId)
                .status(QuizGenerationStatus.COMPLETED).phase(QuizGenerationJob.Phase.DONE).build();
        when(loadParent.getById(parentId)).thenReturn(Optional.of(parent));

        service.expireParent(parentId);

        verify(visualScheduler).cancelChild(childId);
        verify(visualScheduler).cancelParent(parentId);
        verify(timingLogger).visualExpired(eq(expired), eq(parentId),
                eq("VISUAL_DEADLINE_EXCEEDED"), any(LocalDateTime.class));
        verify(timingLogger).visualPhaseTerminal(eq(parent), eq(summary), any(LocalDateTime.class));
        verify(timingLogger).quizTerminal(eq(parent), eq(summary), any(LocalDateTime.class));
        verify(sse).pushStatus(eq(parentId), any());
    }

    @Test
    void lateVisualResponseCannotPublishAfterDeadlineChangedJobState() {
        QuestionVisualJobPort.WorkItem work = workItem();
        String latex = "\\documentclass{article}\\begin{document}x^2\\end{document}";
        String svg = "<svg viewBox=\"0 0 10 10\"><path d=\"M0 0 L5 5\"/></svg>";
        String rendered = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        doReturn(new QuestionVisualGenerationService.VisualResponse(
                work.id(), "LATEX", latex, rendered, "image/svg+xml"))
                .when(service).requestVisual(work);
        when(visualJobs.completeVisual(eq(work.id()), eq(work.imageId()), eq("LATEX"),
                eq(latex), any(), any(), any(), any(Long.class)))
                .thenReturn(false);

        service.process(work);

        verify(visualScheduler, never()).cancelChild(work.id());
        verify(sse, never()).pushStatus(any(), any());
        verify(timingLogger, never()).visualCompleted(any(), any(), any());
    }

    @Test
    void successfulVisualLogsChildPhaseAndWholeQuizAfterWinningTerminalTransitions() {
        QuestionVisualJobPort.WorkItem work = workItem();
        String svg = "<svg viewBox=\"0 0 10 10\"><path d=\"M0 0 L5 5\"/></svg>";
        String rendered = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        doReturn(new QuestionVisualGenerationService.VisualResponse(
                work.id(), "SVG", svg, rendered, "image/svg+xml"))
                .when(service).requestVisual(work);
        when(publicUrl.getPublicImageUrl(any())).thenReturn("https://example.test/visual.svg");
        when(visualJobs.completeVisual(eq(work.id()), eq(work.imageId()), eq("SVG"),
                eq(svg), any(), any(), any(), any(Long.class))).thenReturn(true);
        QuestionVisualJobPort.Summary summary = new QuestionVisualJobPort.Summary(
                1, 1, 0, 0, work.createdAt());
        when(visualJobs.summarize(work.parentJobId())).thenReturn(summary);
        QuizGenerationJob completedParent = QuizGenerationJob.builder()
                .id(work.parentJobId()).quizId(work.quizId()).status(QuizGenerationStatus.COMPLETED)
                .phase(QuizGenerationJob.Phase.DONE).createdAt(work.parentCreatedAt())
                .requestSnapshot("{\"questionCount\":1,\"examMatrix\":[]}").build();
        when(loadParent.getById(work.parentJobId())).thenReturn(Optional.of(completedParent));
        when(saveParent.completeVisualPhase(eq(work.parentJobId()), eq(null), eq(null),
                any(LocalDateTime.class))).thenReturn(true);

        service.process(work);

        verify(timingLogger).visualCompleted(eq(work), eq("SVG"), any(LocalDateTime.class));
        verify(timingLogger).visualPhaseTerminal(eq(completedParent), eq(summary), any(LocalDateTime.class));
        verify(timingLogger).quizTerminal(eq(completedParent), eq(summary), any(LocalDateTime.class));
    }

    private QuestionVisualJobPort.WorkItem workItem() {
        return new QuestionVisualJobPort.WorkItem(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "AUTO", "Question", "Draw it", "Description", 0,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
