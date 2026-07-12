package edu.ai.lms.core.quiz.app.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
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
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobMaterialPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.domain.ExamMatrixRow;
import edu.ai.lms.core.material.app.domain.Material;
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
    private final LoadQuizGenerationJobMaterialPort loadQuizGenerationJobMaterialPort;
    private final LoadExamMatrixPort loadExamMatrixPort;
    private final GetMaterialPort getMaterialPort;
    private final SseQuizGenerationService sseQuizGenerationService;
    private final WebClient aiQuizWebClient;

    @Value("${quiz.generation.webhook.base-url}")
    private String webhookBaseUrl;

    @Value("${quiz.generation.webhook.secret}")
    private String webhookSecret;

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
                        processingJob.getStartTime() != null ? processingJob.getStartTime().toString() : null,
                        processingJob.getDueTime() != null ? processingJob.getDueTime().toString() : null,
                        processingJob.getAttemptLimited(),
                        processingJob.getDisplay()
                );
            }

            Map<UUID, LearningOutcome> outcomeById = learningOutcomes.stream()
                    .collect(Collectors.toMap(LearningOutcome::getId, Function.identity(), (first, ignored) -> first));

            List<ExamMatrixRowContext> examMatrixContext = null;
            if (processingJob.getExamMatrixId() != null) {
                ExamMatrix matrix = loadExamMatrixPort.findById(processingJob.getExamMatrixId())
                        .orElseThrow(() -> new IllegalStateException("Exam matrix not found for quiz generation"));
                if (!processingJob.getCourseId().equals(matrix.getCourseId())) {
                    throw new IllegalStateException("Exam matrix does not belong to the quiz course");
                }
                if (matrix.getRows() != null) {
                    examMatrixContext = matrix.getRows().stream()
                            .map(row -> toExamMatrixContext(processingJob.getCourseId(), row, outcomeById))
                            .toList();
                }
            }

            List<UUID> materialIds = loadQuizGenerationJobMaterialPort.findMaterialIdsByJobId(processingJob.getId());
            List<MaterialContext> materialsContext = null;
            if (materialIds != null && !materialIds.isEmpty()) {
                materialsContext = materialIds.stream()
                        .map(materialId -> getMaterialPort.findByIdAndCourseId(materialId, processingJob.getCourseId()))
                        .filter(java.util.Optional::isPresent)
                        .map(java.util.Optional::get)
                        .map(m -> new MaterialContext(m.getId(), m.getTitle(), m.getFileKey(), m.getType() != null ? m.getType().name() : null))
                        .toList();
            }

            QuizGenerationAiRequest payload = new QuizGenerationAiRequest(
                    processingJob.getId(),
                    processingJob.getCourseId(),
                    course.getCourseName(),
                    processingJob.getChapterId(),
                    chapter != null ? chapter.getTitle() : null,
                    processingJob.getQuestionCount(),
                    processingJob.getQuestionType(),
                    examMatrixContext,
                    materialsContext,
                    learningOutcomes.stream()
                            .map(outcome -> new LearningOutcomeContext(outcome.getCode(), outcome.getDescription()))
                            .toList(),
                    processingJob.getPrompt(),
                    Boolean.TRUE.equals(processingJob.getUseOptions()),
                    processingJob.getStrictMode() == null || Boolean.TRUE.equals(processingJob.getStrictMode()),
                    options,
                    buildWebhookUrl(),
                    webhookSecret
            );

            aiQuizWebClient.post()
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

    private ExamMatrixRowContext toExamMatrixContext(UUID courseId, ExamMatrixRow row, Map<UUID, LearningOutcome> outcomeById) {
        LearningOutcome outcome = outcomeById.get(row.getLearningOutcomeId());
        String clo = outcome != null ? outcome.getCode() : row.getLearningOutcomeId().toString();
        String chapterTitle = resolveChapterTitle(courseId, row.getChapterId());
        String topic = row.getTopic();
        if (topic == null || topic.isBlank()) {
            topic = chapterTitle != null ? chapterTitle : clo;
        }

        return new ExamMatrixRowContext(
                clo,
                row.getLearningOutcomeId(),
                row.getChapterId(),
                chapterTitle,
                topic,
                row.getUnderstandCount(),
                row.getApplyCount(),
                row.getApplyPlusCount()
        );
    }

    private String resolveChapterTitle(UUID courseId, UUID chapterId) {
        if (chapterId == null) {
            return null;
        }
        return loadChapterPort.findByKey(courseId, chapterId)
                .map(Chapter::getTitle)
                .orElse(null);
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

    record ExamMatrixRowContext(
            String clo,
            UUID learningOutcomeId,
            UUID chapterId,
            String chapter,
            String topic,
            int hieu,
            int van_dung,
            int vdc
    ) {}

    record MaterialContext(
            UUID id,
            String title,
            String fileKey,
            String type
    ) {}

    record QuizGenerationAiRequest(
            UUID jobId,
            UUID courseId,
            String courseTitle,
            UUID chapterId,
            String chapterTitle,
            Integer questionCount,
            Question.QuestionType questionType,
            List<ExamMatrixRowContext> examMatrix,
            List<MaterialContext> materials,
            List<LearningOutcomeContext> learningOutcomes,
            String prompt,
            boolean useOptions,
            boolean strictMode,
            QuizGenerationOptions options,
            String webhookUrl,
            String webhookSecret
    ) {
    }

        record QuizGenerationOptions(
            Boolean unlimited,
            Integer timeLimitMinutes,
            Double passScore,
            String startTime,
            String dueTime,
            Integer attemptLimited,
            Boolean display
        ) {
        }
}
