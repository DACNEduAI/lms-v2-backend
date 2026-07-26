package edu.ai.lms.core.quiz.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.adapter.in.http.request.QuizWebhookRequest;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.exception.QuizGenerationJobNotFoundException;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.ValidateLearningOutcomesPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizWebhookProcessingService {
    private final LoadQuizGenerationJobPort loadJobPort;
    private final SaveQuizGenerationJobPort saveJobPort;
    private final SaveQuizPort saveQuizPort;
    private final SaveQuestionPort saveQuestionPort;
    private final SaveAnswerPort saveAnswerPort;
    private final SaveQuizLearningOutcomePort saveQuizLearningOutcomePort;
    private final SaveQuestionLearningOutcomePort saveQuestionLearningOutcomePort;
    private final ValidateLearningOutcomesPort learningOutcomesPort;
    private final ScheduleQuizGenerationJobPort schedulerPort;
    private final SseQuizGenerationService sseService;
    private final ObjectMapper objectMapper;

    @Transactional
    public QuizGenerationStatusResponse handleWebhook(QuizWebhookRequest request) {
        if (request.getJobId() == null || request.getEventId() == null) {
            throw new IllegalArgumentException("jobId and eventId are required");
        }
        QuizGenerationJob job = loadJobPort.getByIdForUpdate(request.getJobId())
                .orElseThrow(() -> new QuizGenerationJobNotFoundException("Quiz generation job not found"));
        if (job.getStatus() == QuizGenerationStatus.COMPLETED || job.getStatus() == QuizGenerationStatus.FAILED) {
            return QuizGenerationService.toStatusResponse(job);
        }
        if (job.getStatus() == QuizGenerationStatus.TIMED_OUT) {
            throw new IllegalStateException("Timed-out generation jobs do not accept callbacks");
        }

        if ("FAILED".equalsIgnoreCase(request.getStatus())) {
            log.warn("AI generation failed for job {} with code {}: {}", job.getId(),
                    request.getErrorCode(), request.getErrorMessage());
            job.setCallbackEventId(request.getEventId());
            job.setStatus(QuizGenerationStatus.FAILED);
            job.setErrorCode(request.getErrorCode() == null ? "GENERATION_FAILED" : request.getErrorCode());
            job.setErrorMessage("Quiz generation could not be completed.");
            job.setLeaseUntil(null);
            job.setCompletedAt(LocalDateTime.now());
            QuizGenerationJob saved = saveJobPort.save(job);
            deferTerminalNotification(saved);
            return QuizGenerationService.toStatusResponse(saved);
        }
        if (!"COMPLETED".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalArgumentException("Webhook status must be COMPLETED or FAILED");
        }

        validatePayload(job, request);
        Quiz savedQuiz = persistQuiz(job, request.getQuiz());
        job.setQuizId(savedQuiz.getId());
        job.setCallbackEventId(request.getEventId());
        job.setStatus(QuizGenerationStatus.COMPLETED);
        job.setErrorCode(null);
        job.setErrorMessage(null);
        job.setLeaseUntil(null);
        job.setCompletedAt(LocalDateTime.now());
        QuizGenerationJob completed = saveJobPort.save(job);
        deferTerminalNotification(completed);
        return QuizGenerationService.toStatusResponse(completed);
    }

    private void validatePayload(QuizGenerationJob job, QuizWebhookRequest request) {
        if (request.getQuiz() == null || request.getQuiz().getQuestions() == null) {
            throw new IllegalArgumentException("Quiz payload is missing");
        }
        try {
            JsonNode snapshot = objectMapper.readTree(job.getRequestSnapshot());
            int expected = snapshot.path("questionCount").asInt();
            if (request.getQuiz().getQuestions().size() != expected) {
                throw new IllegalArgumentException("Generated question count does not match the accepted matrix");
            }
            Map<String, Integer> expectedDistribution = new HashMap<>();
            snapshot.path("examMatrix").forEach(row -> {
                String outcomeId = row.path("learningOutcomeId").asText();
                addExpected(expectedDistribution, outcomeId, "MULTIPLE_CHOICE", row.path("hieu").asInt());
                addExpected(expectedDistribution, outcomeId, "SHORT_ANSWER", row.path("van_dung").asInt());
                addExpected(expectedDistribution, outcomeId, "CODE_RUNNER", row.path("vdc").asInt());
            });
            Map<String, Integer> actualDistribution = new HashMap<>();
            for (QuizWebhookRequest.QuestionPayload question : request.getQuiz().getQuestions()) {
                if (question.getContent() == null || question.getContent().isBlank()) {
                    throw new IllegalArgumentException("Generated question content is required");
                }
                if (question.getLearningOutcomeIds() == null || question.getLearningOutcomeIds().size() != 1
                        || question.getOriginalType() == null || question.getOriginalType().isBlank()) {
                    throw new IllegalArgumentException("Every generated question must identify one matrix outcome and original type");
                }
                boolean multipleChoice = "MULTIPLE_CHOICE".equalsIgnoreCase(question.getOriginalType());
                boolean codeRunner = "CODE_RUNNER".equalsIgnoreCase(question.getOriginalType());
                Question.QuestionType expectedLmsType = multipleChoice
                        ? Question.QuestionType.MULTIPLE_CHOICE
                        : codeRunner ? Question.QuestionType.CODE_RUNNER : Question.QuestionType.ESSAY;
                if (question.getType() != expectedLmsType) {
                    throw new IllegalArgumentException("Generated question type conversion is invalid");
                }
                if (multipleChoice && (question.getAnswers() == null || question.getAnswers().size() < 2
                        || question.getAnswers().stream().noneMatch(answer -> Boolean.TRUE.equals(answer.getIsCorrect())))) {
                    throw new IllegalArgumentException("Multiple-choice questions require answer options and a correct answer");
                }
                String key = distributionKey(question.getLearningOutcomeIds().get(0).toString(),
                        question.getOriginalType());
                actualDistribution.merge(key, 1, Integer::sum);
            }
            if (!expectedDistribution.equals(actualDistribution)) {
                throw new IllegalArgumentException("Generated questions do not match the accepted matrix distribution");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not validate the request snapshot", ex);
        }
    }

    private static void addExpected(Map<String, Integer> distribution, String outcomeId, String type, int count) {
        if (count > 0) {
            distribution.merge(distributionKey(outcomeId, type), count, Integer::sum);
        }
    }

    private static String distributionKey(String outcomeId, String type) {
        return outcomeId + "|" + type.toUpperCase();
    }

    private Quiz persistQuiz(QuizGenerationJob job, QuizWebhookRequest.QuizPayload payload) {
        Quiz quiz = Quiz.builder()
                .quizTitle(job.getQuizTitle() == null || job.getQuizTitle().isBlank()
                        ? payload.getQuizTitle() : job.getQuizTitle())
                .courseId(job.getCourseId())
                .teacherId(job.getRequestedRole() == Roles.TEACHER ? job.getRequestedBy() : null)
                .studentId(job.getRequestedRole() == Roles.STUDENT ? job.getRequestedBy() : null)
                .unlimited(Boolean.TRUE.equals(job.getUnlimited()))
                .timeLimitMinutes(job.getTimeLimitMinutes())
                .passScore(job.getPassScore())
                .startTime(Boolean.TRUE.equals(job.getUnlimited()) ? null : job.getStartTime())
                .dueTime(Boolean.TRUE.equals(job.getUnlimited()) ? null : job.getDueTime())
                .attemptLimited(job.getAttemptLimited())
                .display(false)
                .build();
        Quiz savedQuiz = saveQuizPort.save(quiz);
        LinkedHashSet<UUID> quizOutcomeIds = new LinkedHashSet<>();

        for (QuizWebhookRequest.QuestionPayload item : payload.getQuestions()) {
            List<UUID> outcomeIds = item.getLearningOutcomeIds() == null ? List.of() : item.getLearningOutcomeIds();
            if (!learningOutcomesPort.allBelongToCourse(job.getCourseId(), outcomeIds)) {
                throw new IllegalArgumentException("Learning outcome is outside the job course");
            }
            Question savedQuestion = saveQuestionPort.save(Question.builder()
                    .content(item.getContent())
                    .type(item.getType() == null ? Question.QuestionType.ESSAY : item.getType())
                    .quizId(savedQuiz.getId()).build());
            if (!outcomeIds.isEmpty()) {
                saveQuestionLearningOutcomePort.addLearningOutcomes(savedQuestion.getId(), outcomeIds);
                quizOutcomeIds.addAll(outcomeIds);
            }
            if (item.getAnswers() != null) {
                for (QuizWebhookRequest.AnswerPayload answerItem : item.getAnswers()) {
                    saveAnswerPort.save(Answer.builder().questionId(savedQuestion.getId())
                            .content(answerItem.getContent())
                            .isCorrect(Boolean.TRUE.equals(answerItem.getIsCorrect()))
                            .explanation(answerItem.getExplanation()).build());
                }
            }
        }
        if (!quizOutcomeIds.isEmpty()) {
            saveQuizLearningOutcomePort.addLearningOutcomes(savedQuiz.getId(), List.copyOf(quizOutcomeIds));
        }
        return savedQuiz;
    }

    private void deferTerminalNotification(QuizGenerationJob job) {
        QuizGenerationStatusResponse response = QuizGenerationService.toStatusResponse(job);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    schedulerPort.unschedule(job.getId());
                } catch (RuntimeException ignored) {
                }
                sseService.pushStatus(job.getId(), response);
            }
        });
    }
}
