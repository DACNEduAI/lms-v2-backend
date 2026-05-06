package edu.ai.lms.core.quiz.app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.exception.QuizGenerationJobNotFoundException;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizGenerationProcessingService {

    private final LoadQuizGenerationJobPort loadQuizGenerationJobPort;
    private final SaveQuizGenerationJobPort saveQuizGenerationJobPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadChapterPort loadChapterPort;
    private final LoadLearningOutcomePort loadLearningOutcomePort;
    private final LoadQuizGenerationJobLearningOutcomePort loadQuizGenerationJobLearningOutcomePort;
    private final SseQuizGenerationService sseQuizGenerationService;
    private final WebClient aiWebClient;

    @Value("${quiz.generation.webhook.base-url}")
    private String webhookBaseUrl;

    @Value("${quiz.generation.webhook.secret}")
    private String webhookSecret;

    @Transactional
    public void process(UUID jobId) {
        QuizGenerationJob job = loadQuizGenerationJobPort.getById(jobId)
                .orElseThrow(() -> new QuizGenerationJobNotFoundException("Quiz generation job not found"));

        if (job.getStatus() == QuizGenerationStatus.COMPLETED || job.getStatus() == QuizGenerationStatus.FAILED) {
            return;
        }

        try {
            job.setStatus(QuizGenerationStatus.PROCESSING);
            job.setErrorMessage(null);
            QuizGenerationJob processingJob = saveQuizGenerationJobPort.save(job);
            sseQuizGenerationService.pushStatus(jobId, toStatusResponse(processingJob));

            Course course = loadCoursePort.findById(processingJob.getCourseId())
                    .orElseThrow(() -> new IllegalStateException("Course not found for quiz generation"));

            Chapter chapter = null;
            if (processingJob.getChapterId() != null) {
                chapter = loadChapterPort.findByKey(processingJob.getCourseId(), processingJob.getChapterId())
                        .orElseThrow(() -> new IllegalStateException("Chapter not found for quiz generation"));
            }

            List<LearningOutcome> learningOutcomes = resolveLearningOutcomes(processingJob, chapter);

            QuizGenerationOptions options = null;
            if (Boolean.TRUE.equals(processingJob.getUseOptions())) {
                options = new QuizGenerationOptions(
                        processingJob.getUnlimited(),
                        processingJob.getTimeLimitMinutes(),
                        processingJob.getPassScore(),
                        processingJob.getStartTime(),
                        processingJob.getDueTime(),
                        processingJob.getAttemptLimited(),
                        processingJob.getDisplay()
                );
            }

            QuizGenerationAiRequest payload = new QuizGenerationAiRequest(
                    processingJob.getId(),
                    processingJob.getCourseId(),
                    course.getCourseName(),
                    processingJob.getChapterId(),
                    chapter != null ? chapter.getTitle() : null,
                    processingJob.getQuestionCount(),
                    processingJob.getQuestionType(),
                    learningOutcomes.stream()
                            .map(outcome -> new LearningOutcomeContext(outcome.getCode(), outcome.getDescription()))
                            .toList(),
                        processingJob.getPrompt(),
                        Boolean.TRUE.equals(processingJob.getUseOptions()),
                        options,
                    buildWebhookUrl(),
                    webhookSecret
            );

            aiWebClient.post()
                    .uri("/quiz-gen/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception ex) {
            log.error("Quiz generation job {} failed to start", jobId, ex);
            job.setStatus(QuizGenerationStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            QuizGenerationJob failedJob = saveQuizGenerationJobPort.save(job);
            sseQuizGenerationService.pushStatus(jobId, toStatusResponse(failedJob));
        }
    }

    private List<LearningOutcome> resolveLearningOutcomes(QuizGenerationJob job, Chapter chapter) {
        List<UUID> requestedOutcomeIds = loadQuizGenerationJobLearningOutcomePort
                .findLearningOutcomeIdsByJobId(job.getId());
        List<LearningOutcome> courseOutcomes = loadLearningOutcomePort.findByCourseId(job.getCourseId());

        if (Boolean.TRUE.equals(job.getUseOptions()) && !requestedOutcomeIds.isEmpty()) {
            return courseOutcomes.stream()
                    .filter(outcome -> requestedOutcomeIds.contains(outcome.getId()))
                    .toList();
        }

        if (chapter == null || chapter.getLearningOutcomeIds() == null || chapter.getLearningOutcomeIds().isEmpty()) {
            return courseOutcomes;
        }

        return courseOutcomes.stream()
                .filter(outcome -> chapter.getLearningOutcomeIds().contains(outcome.getId()))
                .toList();
    }

    private String buildWebhookUrl() {
        String trimmedBase = webhookBaseUrl.endsWith("/")
                ? webhookBaseUrl.substring(0, webhookBaseUrl.length() - 1)
                : webhookBaseUrl;
        return trimmedBase + "/api/internal/webhooks/quiz-ready";
    }

    private QuizGenerationStatusResponse toStatusResponse(QuizGenerationJob job) {
        return QuizGenerationStatusResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .quizId(job.getQuizId())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    record LearningOutcomeContext(String code, String description) {
    }

    record QuizGenerationAiRequest(
            UUID jobId,
            UUID courseId,
            String courseTitle,
            UUID chapterId,
            String chapterTitle,
            Integer questionCount,
            Question.QuestionType questionType,
            List<LearningOutcomeContext> learningOutcomes,
            String prompt,
            boolean useOptions,
            QuizGenerationOptions options,
            String webhookUrl,
            String webhookSecret
    ) {
    }

        record QuizGenerationOptions(
            Boolean unlimited,
            Integer timeLimitMinutes,
            Double passScore,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime dueTime,
            Integer attemptLimited,
            Boolean display
        ) {
        }
}
