package edu.ai.lms.core.quiz.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.quiz.adapter.in.http.request.QuizWebhookRequest;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuizGenerationJobPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizWebhookProcessingServiceUT {
    @Mock private LoadQuizGenerationJobPort loadJobPort;
    @Mock private SaveQuizGenerationJobPort saveJobPort;
    @Mock private SaveQuizPort saveQuizPort;
    @Mock private SaveQuestionPort saveQuestionPort;
    @Mock private SaveAnswerPort saveAnswerPort;
    @Mock private SaveQuizLearningOutcomePort saveQuizLearningOutcomePort;
    @Mock private SaveQuestionLearningOutcomePort saveQuestionLearningOutcomePort;
    @Mock private LoadLearningOutcomePort learningOutcomePort;
    @Mock private ScheduleQuizGenerationJobPort schedulerPort;
    @Mock private SseQuizGenerationService sseService;

    private QuizWebhookProcessingService service;

    @BeforeEach
    void setUp() {
        service = new QuizWebhookProcessingService(loadJobPort, saveJobPort, saveQuizPort,
                saveQuestionPort, saveAnswerPort, saveQuizLearningOutcomePort,
                saveQuestionLearningOutcomePort, learningOutcomePort, schedulerPort,
                sseService, new ObjectMapper());
    }

    @Test
    void duplicateCompletedCallbackReturnsExistingQuiz() {
        UUID jobId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        QuizGenerationJob job = QuizGenerationJob.builder().id(jobId).quizId(quizId)
                .status(QuizGenerationStatus.COMPLETED).build();
        when(loadJobPort.getByIdForUpdate(jobId)).thenReturn(Optional.of(job));

        var response = service.handleWebhook(QuizWebhookRequest.builder()
                .eventId(UUID.randomUUID()).jobId(jobId).status("COMPLETED").build());

        assertEquals(QuizGenerationStatus.COMPLETED, response.getStatus());
        assertEquals(quizId, response.getQuizId());
        verify(saveQuizPort, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsQuestionsOutsideAcceptedMatrixDistribution() {
        UUID jobId = UUID.randomUUID();
        UUID outcomeId = UUID.randomUUID();
        String snapshot = """
                {"questionCount":1,"examMatrix":[{"learningOutcomeId":"%s","hieu":1,"van_dung":0,"vdc":0}]}
                """.formatted(outcomeId);
        QuizGenerationJob job = QuizGenerationJob.builder().id(jobId)
                .status(QuizGenerationStatus.PROCESSING).requestSnapshot(snapshot).build();
        when(loadJobPort.getByIdForUpdate(jobId)).thenReturn(Optional.of(job));

        QuizWebhookRequest.QuestionPayload question = QuizWebhookRequest.QuestionPayload.builder()
                .content("Wrong matrix type").type(Question.QuestionType.ESSAY)
                .originalType("SHORT_ANSWER").learningOutcomeIds(List.of(outcomeId)).build();
        QuizWebhookRequest request = QuizWebhookRequest.builder().eventId(UUID.randomUUID())
                .jobId(jobId).status("COMPLETED")
                .quiz(QuizWebhookRequest.QuizPayload.builder().questions(List.of(question)).build())
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.handleWebhook(request));
        verify(saveQuizPort, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
