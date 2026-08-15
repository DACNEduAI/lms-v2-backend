package edu.ai.lms.core.quiz.app.service.genquiz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.quiz.adapter.in.http.request.QuizGenerationProgressWebhookRequest;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuizGenerationProgressWebhookUT {

    @Test
    void acceptsOnlyMonotonicProgressForTheCurrentTextAttempt() {
        UUID jobId = UUID.randomUUID();
        QuizGenerationJob job = QuizGenerationJob.builder()
                .id(jobId).status(QuizGenerationStatus.PROCESSING)
                .phase(QuizGenerationJob.Phase.TEXT_GENERATION)
                .attemptCount(2).generatedQuestionCount(1).totalQuestionCount(3).build();
        LoadQuizGenerationJobPort load = mock(LoadQuizGenerationJobPort.class);
        SaveQuizGenerationJobPort save = mock(SaveQuizGenerationJobPort.class);
        when(load.getByIdForUpdate(jobId)).thenReturn(Optional.of(job));
        when(save.save(job)).thenReturn(job);
        QuizWebhookProcessingService service = new QuizWebhookProcessingService(
                load, save, null, null, null, null, null, null, null, null,
                new ObjectMapper(), null, null, null, Clock.systemUTC());

        service.handleProgress(QuizGenerationProgressWebhookRequest.builder()
                .jobId(jobId).attempt(2).generatedQuestionCount(2).build());
        service.handleProgress(QuizGenerationProgressWebhookRequest.builder()
                .jobId(jobId).attempt(2).generatedQuestionCount(1).build());
        service.handleProgress(QuizGenerationProgressWebhookRequest.builder()
                .jobId(jobId).attempt(1).generatedQuestionCount(3).build());
        service.handleProgress(QuizGenerationProgressWebhookRequest.builder()
                .jobId(jobId).attempt(2).generatedQuestionCount(4).build());
        job.setStatus(QuizGenerationStatus.COMPLETED);
        service.handleProgress(QuizGenerationProgressWebhookRequest.builder()
                .jobId(jobId).attempt(2).generatedQuestionCount(3).build());

        assertEquals(2, job.getGeneratedQuestionCount());
        verify(save, times(1)).save(job);
    }
}
