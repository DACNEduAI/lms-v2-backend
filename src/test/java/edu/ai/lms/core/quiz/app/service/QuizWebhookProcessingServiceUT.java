package edu.ai.lms.core.quiz.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.quiz.adapter.in.http.request.QuizWebhookRequest;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
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
import edu.ai.lms.core.quiz.app.port.out.ValidateLearningOutcomesPort;
import edu.ai.lms.core.quiz.app.port.out.QuestionVisualJobPort;
import edu.ai.lms.core.quiz.app.service.genquiz.QuestionVisualGenerationService;
import edu.ai.lms.core.quiz.app.service.genquiz.QuizWebhookProcessingService;
import edu.ai.lms.core.quiz.app.service.genquiz.QuizGenerationTimingLogger;
import edu.ai.lms.core.quiz.app.service.genquiz.SseQuizGenerationService;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class QuizWebhookProcessingServiceUT {
    @Mock private LoadQuizGenerationJobPort loadJobPort;
    @Mock private SaveQuizGenerationJobPort saveJobPort;
    @Mock private SaveQuizPort saveQuizPort;
    @Mock private SaveQuestionPort saveQuestionPort;
    @Mock private SaveAnswerPort saveAnswerPort;
    @Mock private SaveQuizLearningOutcomePort saveQuizLearningOutcomePort;
    @Mock private SaveQuestionLearningOutcomePort saveQuestionLearningOutcomePort;
    @Mock private ValidateLearningOutcomesPort learningOutcomesPort;
    @Mock private ScheduleQuizGenerationJobPort schedulerPort;
    @Mock private SseQuizGenerationService sseService;
    @Mock private QuestionVisualJobPort visualJobs;
    @Mock private QuestionVisualGenerationService visualGenerationService;
    @Mock private QuizGenerationTimingLogger timingLogger;

    private QuizWebhookProcessingService service;

    @BeforeEach
    void setUp() {
        service = new QuizWebhookProcessingService(loadJobPort, saveJobPort, saveQuizPort,
                saveQuestionPort, saveAnswerPort, saveQuizLearningOutcomePort,
                saveQuestionLearningOutcomePort, learningOutcomesPort, schedulerPort,
                sseService, new ObjectMapper(), visualJobs, visualGenerationService, timingLogger);
        lenient().when(visualJobs.summarize(any())).thenReturn(
                new QuestionVisualJobPort.Summary(0, 0, 0, 0, null));
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
        verifyNoInteractions(timingLogger);
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

    @Test
    void acceptsMixedTypesAndAllApplicableOutcomesForDynamicRequirement() {
        UUID jobId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        UUID firstOutcomeId = UUID.randomUUID();
        UUID secondOutcomeId = UUID.randomUUID();
        String snapshot = """
                {
                  "snapshotVersion":2,
                  "questionCount":2,
                  "examMatrix":[{
                    "rowId":"%s",
                    "levelCounts":[{
                      "customLearningLevelId":"%s",
                      "learningOutcomeIds":["%s","%s"],
                      "questionCount":2
                    }]
                  }]
                }
                """.formatted(rowId, levelId, firstOutcomeId, secondOutcomeId);
        QuizGenerationJob job = QuizGenerationJob.builder().id(jobId).courseId(courseId)
                .status(QuizGenerationStatus.PROCESSING).requestSnapshot(snapshot).build();
        when(loadJobPort.getByIdForUpdate(jobId)).thenReturn(Optional.of(job));
        when(saveQuizPort.save(any())).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            quiz.setId(UUID.randomUUID());
            return quiz;
        });
        when(saveQuestionPort.save(any())).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(UUID.randomUUID());
            return question;
        });
        when(learningOutcomesPort.allBelongToCourse(
                org.mockito.ArgumentMatchers.eq(courseId), any())).thenReturn(true);
        when(saveJobPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        QuizWebhookRequest.QuestionPayload multipleChoice =
                dynamicQuestion(rowId, levelId, firstOutcomeId, secondOutcomeId)
                        .type(Question.QuestionType.MULTIPLE_CHOICE)
                        .originalType("MULTIPLE_CHOICE")
                        .answers(List.of(
                                QuizWebhookRequest.AnswerPayload.builder()
                                        .content("A").isCorrect(true).build(),
                                QuizWebhookRequest.AnswerPayload.builder()
                                        .content("B").isCorrect(false).build()))
                        .build();
        QuizWebhookRequest.QuestionPayload shortAnswer =
                dynamicQuestion(rowId, levelId, secondOutcomeId, firstOutcomeId)
                        .type(Question.QuestionType.ESSAY)
                        .originalType("SHORT_ANSWER")
                        .build();
        QuizWebhookRequest request = QuizWebhookRequest.builder()
                .eventId(UUID.randomUUID()).jobId(jobId).status("COMPLETED")
                .quiz(QuizWebhookRequest.QuizPayload.builder()
                        .questions(List.of(multipleChoice, shortAnswer)).build())
                .build();

        var response = service.handleWebhook(request);

        assertEquals(QuizGenerationStatus.COMPLETED, response.getStatus());
        verify(saveQuestionLearningOutcomePort,
                org.mockito.Mockito.times(2))
                .addLearningOutcomes(any(), any());
    }

    @Test
    void rejectsDynamicQuestionWithWrongProvenanceOrOutcomeSet() {
        UUID jobId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        UUID outcomeId = UUID.randomUUID();
        String snapshot = """
                {
                  "snapshotVersion":2,
                  "questionCount":1,
                  "examMatrix":[{
                    "rowId":"%s",
                    "levelCounts":[{
                      "customLearningLevelId":"%s",
                      "learningOutcomeIds":["%s"],
                      "questionCount":1
                    }]
                  }]
                }
                """.formatted(rowId, levelId, outcomeId);
        QuizGenerationJob job = QuizGenerationJob.builder().id(jobId)
                .status(QuizGenerationStatus.PROCESSING).requestSnapshot(snapshot).build();
        when(loadJobPort.getByIdForUpdate(jobId)).thenReturn(Optional.of(job));
        QuizWebhookRequest.QuestionPayload question =
                dynamicQuestion(rowId, UUID.randomUUID(), outcomeId)
                        .type(Question.QuestionType.ESSAY)
                        .originalType("SHORT_ANSWER")
                        .build();

        QuizWebhookRequest request = QuizWebhookRequest.builder()
                .eventId(UUID.randomUUID()).jobId(jobId).status("COMPLETED")
                .quiz(QuizWebhookRequest.QuizPayload.builder()
                        .questions(List.of(question)).build())
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> service.handleWebhook(request));
        verify(saveQuizPort, never()).save(any());
    }

    @Test
    void acceptsVersionThreeQuestionMatchingCcctAllocation() {
        UUID jobId = UUID.randomUUID(), courseId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID(), itemId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        String snapshot = """
                {
                  "snapshotVersion":3,
                  "questionCount":1,
                  "examMatrix":[{
                    "rowId":"%s",
                    "ccctItemId":"%s",
                    "levelCounts":[{
                      "customLearningLevelId":"%s",
                      "learningOutcomeIds":["%s"],
                      "questionType":"MULTIPLE_CHOICE",
                      "questionCount":1
                    }]
                  }]
                }
                """.formatted(rowId, itemId, levelId, outcomeId);
        QuizGenerationJob job = QuizGenerationJob.builder().id(jobId).courseId(courseId)
                .status(QuizGenerationStatus.PROCESSING).requestSnapshot(snapshot).build();
        when(loadJobPort.getByIdForUpdate(jobId)).thenReturn(Optional.of(job));
        when(saveQuizPort.save(any())).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            quiz.setId(UUID.randomUUID());
            return quiz;
        });
        when(saveQuestionPort.save(any())).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(UUID.randomUUID());
            return question;
        });
        when(learningOutcomesPort.allBelongToCourse(courseId, List.of(outcomeId))).thenReturn(true);
        when(saveJobPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        QuizWebhookRequest.QuestionPayload question = dynamicQuestion(rowId, levelId, outcomeId)
                .ccctItemId(itemId)
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .originalType("MULTIPLE_CHOICE")
                .answers(List.of(
                        QuizWebhookRequest.AnswerPayload.builder().content("A").isCorrect(true).build(),
                        QuizWebhookRequest.AnswerPayload.builder().content("B").isCorrect(false).build()))
                .build();
        QuizWebhookRequest request = QuizWebhookRequest.builder()
                .eventId(UUID.randomUUID()).jobId(jobId).status("COMPLETED")
                .quiz(QuizWebhookRequest.QuizPayload.builder().questions(List.of(question)).build())
                .build();

        assertEquals(QuizGenerationStatus.COMPLETED, service.handleWebhook(request).getStatus());
    }

    @Test
    void acceptsVersionFourQuestionMatchingMatrixAllocation() {
        UUID jobId = UUID.randomUUID(), courseId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        String snapshot = """
                {
                  "snapshotVersion":4,
                  "questionCount":1,
                  "examMatrix":[{
                    "rowId":"%s",
                    "levelCounts":[{
                      "customLearningLevelId":"%s",
                      "learningOutcomeIds":["%s"],
                      "questionType":"MULTIPLE_CHOICE",
                      "questionCount":1
                    }]
                  }]
                }
                """.formatted(rowId, levelId, outcomeId);
        QuizGenerationJob job = QuizGenerationJob.builder().id(jobId).courseId(courseId)
                .status(QuizGenerationStatus.PROCESSING).requestSnapshot(snapshot).build();
        when(loadJobPort.getByIdForUpdate(jobId)).thenReturn(Optional.of(job));
        when(saveQuizPort.save(any())).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            quiz.setId(UUID.randomUUID());
            return quiz;
        });
        when(saveQuestionPort.save(any())).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(UUID.randomUUID());
            return question;
        });
        when(learningOutcomesPort.allBelongToCourse(courseId, List.of(outcomeId))).thenReturn(true);
        when(saveJobPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        QuizWebhookRequest.QuestionPayload question = dynamicQuestion(rowId, levelId, outcomeId)
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .originalType("MULTIPLE_CHOICE")
                .answers(List.of(
                        QuizWebhookRequest.AnswerPayload.builder().content("A").isCorrect(true).build(),
                        QuizWebhookRequest.AnswerPayload.builder().content("B").isCorrect(false).build()))
                .build();
        QuizWebhookRequest request = QuizWebhookRequest.builder()
                .eventId(UUID.randomUUID()).jobId(jobId).status("COMPLETED")
                .quiz(QuizWebhookRequest.QuizPayload.builder().questions(List.of(question)).build())
                .build();

        assertEquals(QuizGenerationStatus.COMPLETED, service.handleWebhook(request).getStatus());
        verify(visualGenerationService, never()).requestDispatchAfterCommit(any(), any());
        verify(timingLogger).textTerminal(any(), eq("completed"), eq(null), any());
        verify(timingLogger).quizTerminal(any(), any(), any());
    }

    @Test
    void visualJobsDispatchOnlyAfterTextCallbackCreatesDurableWork() {
        UUID jobId = UUID.randomUUID(), courseId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        String snapshot = """
                {
                  "snapshotVersion":4,
                  "questionCount":1,
                  "examMatrix":[{
                    "rowId":"%s",
                    "levelCounts":[{
                      "customLearningLevelId":"%s",
                      "learningOutcomeIds":["%s"],
                      "questionType":"MULTIPLE_CHOICE",
                      "questionCount":1,
                      "imageQuestionCount":1
                    }]
                  }]
                }
                """.formatted(rowId, levelId, outcomeId);
        QuizGenerationJob job = QuizGenerationJob.builder().id(jobId).courseId(courseId)
                .status(QuizGenerationStatus.PROCESSING)
                .phase(QuizGenerationJob.Phase.TEXT_GENERATION)
                .requestSnapshot(snapshot).build();
        when(loadJobPort.getByIdForUpdate(jobId)).thenReturn(Optional.of(job));
        when(saveQuizPort.save(any())).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            quiz.setId(UUID.randomUUID());
            return quiz;
        });
        when(saveQuestionPort.save(any())).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(UUID.randomUUID());
            return question;
        });
        when(learningOutcomesPort.allBelongToCourse(courseId, List.of(outcomeId))).thenReturn(true);
        when(saveJobPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(visualJobs.summarize(jobId)).thenReturn(
                new QuestionVisualJobPort.Summary(1, 0, 0, 1, java.time.LocalDateTime.now(java.time.Clock.systemUTC())));

        QuizWebhookRequest.QuestionPayload question = dynamicQuestion(rowId, levelId, outcomeId)
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .originalType("MULTIPLE_CHOICE")
                .answers(List.of(
                        QuizWebhookRequest.AnswerPayload.builder().content("A").isCorrect(true).build(),
                        QuizWebhookRequest.AnswerPayload.builder().content("B").isCorrect(false).build()))
                .visual(QuizWebhookRequest.VisualPayload.builder()
                        .type("AUTO").brief("Draw a diagram").altText("A diagram").build())
                .build();
        QuizWebhookRequest request = QuizWebhookRequest.builder()
                .eventId(UUID.randomUUID()).jobId(jobId).status("COMPLETED")
                .quiz(QuizWebhookRequest.QuizPayload.builder().questions(List.of(question)).build())
                .build();

        var response = service.handleWebhook(request);

        assertEquals(QuizGenerationJob.Phase.VISUAL_GENERATION, response.getPhase());
        verify(visualJobs).create(eq(jobId), any(UUID.class), eq("AUTO"));
        verify(visualGenerationService).requestDispatchAfterCommit(eq(jobId), any());
        verify(timingLogger).textTerminal(any(), eq("completed"), eq(null), any());
        verify(timingLogger, never()).quizTerminal(any(), any(), any());
    }

    private QuizWebhookRequest.QuestionPayload.QuestionPayloadBuilder dynamicQuestion(
            UUID rowId, UUID levelId, UUID... outcomeIds) {
        return QuizWebhookRequest.QuestionPayload.builder()
                .content("Generated question")
                .matrixRowId(rowId)
                .customLearningLevelId(levelId)
                .learningOutcomeIds(List.of(outcomeIds));
    }
}
