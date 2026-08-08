package edu.ai.lms.core.quiz.app.service.genquiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.adapter.in.http.request.QuizWebhookRequest;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.QuestionReviewStatus;
import edu.ai.lms.core.quiz.app.domain.QuestionSourceType;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.domain.QuizLifecycleStatus;
import edu.ai.lms.core.quiz.app.domain.QuizSourceType;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.exception.QuizGenerationJobNotFoundException;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.QuestionVisualJobPort;
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
import java.util.Set;
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
    private final QuestionVisualJobPort visualJobs;
    private final QuestionVisualGenerationService visualGenerationService;

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
        job.setLeaseUntil(null);
        QuestionVisualJobPort.Summary visualSummary = visualJobs.summarize(job.getId());
        if (visualSummary.requested() > 0) {
            job.setStatus(QuizGenerationStatus.PROCESSING);
            job.setPhase(QuizGenerationJob.Phase.VISUAL_GENERATION);
            job.setErrorCode(null);
            job.setErrorMessage(null);
            QuizGenerationJob processing = saveJobPort.save(job);
            visualGenerationService.requestDispatchAfterCommit(job.getId(), job.getCreatedAt());
            return QuizGenerationService.toStatusResponse(processing);
        }
        job.setStatus(QuizGenerationStatus.COMPLETED);
        job.setPhase(QuizGenerationJob.Phase.DONE);
        job.setErrorCode(null);
        job.setErrorMessage(null);
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
            int snapshotVersion = snapshot.path("snapshotVersion").asInt(1);
            if (snapshotVersion >= 4) {
                validateMatrixDistribution(snapshot, request.getQuiz().getQuestions());
            } else if (snapshotVersion == 3) {
                validateCcctDistribution(snapshot, request.getQuiz().getQuestions());
            } else if (snapshotVersion >= 2) {
                validateDynamicDistribution(snapshot, request.getQuiz().getQuestions());
            } else {
                validateLegacyDistribution(snapshot, request.getQuiz().getQuestions());
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not validate the request snapshot", ex);
        }
    }

    private void validateCcctDistribution(
            JsonNode snapshot,
            List<QuizWebhookRequest.QuestionPayload> questions) {
        Map<String, CcctRequirement> expectedRequirements = new HashMap<>();
        snapshot.path("examMatrix").forEach(row -> row.path("levelCounts").forEach(level -> {
            String rowId = row.path("rowId").asText();
            String levelId = level.path("customLearningLevelId").asText();
            String questionType = level.path("questionType").asText();
            LinkedHashSet<UUID> outcomeIds = new LinkedHashSet<>();
            level.path("learningOutcomeIds").forEach(id -> outcomeIds.add(UUID.fromString(id.asText())));
            expectedRequirements.put(
                    matrixKey(rowId, levelId, questionType),
                    new CcctRequirement(
                            level.path("questionCount").asInt(),
                            UUID.fromString(row.path("ccctItemId").asText()),
                            questionType,
                            outcomeIds));
        }));

        Map<String, Integer> actualDistribution = new HashMap<>();
        for (QuizWebhookRequest.QuestionPayload question : questions) {
            validateQuestionShape(question);
            if (!"NONE".equals(visualType(question))) {
                throw new IllegalArgumentException("Contract version 3 does not permit generated visuals");
            }
            if (question.getMatrixRowId() == null
                    || question.getCcctItemId() == null
                    || question.getCustomLearningLevelId() == null) {
                throw new IllegalArgumentException(
                        "Every generated question must identify its matrix row, CCCT item, and custom learning level");
            }
            String type = question.getOriginalType().toUpperCase();
            String key = matrixKey(
                    question.getMatrixRowId().toString(),
                    question.getCustomLearningLevelId().toString(),
                    type);
            CcctRequirement requirement = expectedRequirements.get(key);
            if (requirement == null) {
                throw new IllegalArgumentException(
                        "Generated question references an unknown Matrix allocation");
            }
            if (!requirement.ccctItemId().equals(question.getCcctItemId())
                    || !requirement.questionType().equals(type)) {
                throw new IllegalArgumentException(
                        "Generated question CCCT item or type does not match the accepted Matrix");
            }
            List<UUID> suppliedIds = question.getLearningOutcomeIds();
            LinkedHashSet<UUID> suppliedSet = new LinkedHashSet<>(suppliedIds);
            if (suppliedIds.size() != suppliedSet.size()
                    || !suppliedSet.equals(requirement.learningOutcomeIds())) {
                throw new IllegalArgumentException(
                        "Generated question learning outcomes do not match the accepted Matrix requirement");
            }
            actualDistribution.merge(key, 1, Integer::sum);
        }
        Map<String, Integer> expectedDistribution = new HashMap<>();
        expectedRequirements.forEach((key, requirement) ->
                expectedDistribution.put(key, requirement.questionCount()));
        if (!expectedDistribution.equals(actualDistribution)) {
            throw new IllegalArgumentException(
                    "Generated questions do not match the accepted item, level, type, and count distribution");
        }
    }

    private void validateMatrixDistribution(
            JsonNode snapshot,
            List<QuizWebhookRequest.QuestionPayload> questions) {
        Map<String, MatrixRequirement> expectedRequirements = new HashMap<>();
        snapshot.path("examMatrix").forEach(row -> row.path("levelCounts").forEach(level -> {
            String rowId = row.path("rowId").asText();
            String levelId = level.path("customLearningLevelId").asText();
            String questionType = level.path("questionType").asText();
            LinkedHashSet<UUID> outcomeIds = new LinkedHashSet<>();
            level.path("learningOutcomeIds").forEach(id -> outcomeIds.add(UUID.fromString(id.asText())));
            expectedRequirements.put(
                    matrixKey(rowId, levelId, questionType),
                    new MatrixRequirement(
                            level.path("questionCount").asInt(),
                            level.path("imageQuestionCount").asInt(0),
                            questionType,
                            outcomeIds));
        }));

        Map<String, Integer> actualDistribution = new HashMap<>();
        Map<String, Integer> actualVisualDistribution = new HashMap<>();
        for (QuizWebhookRequest.QuestionPayload question : questions) {
            validateQuestionShape(question);
            if (question.getMatrixRowId() == null || question.getCustomLearningLevelId() == null) {
                throw new IllegalArgumentException(
                        "Every generated question must identify its matrix row and custom learning level");
            }
            String type = question.getOriginalType().toUpperCase();
            String key = matrixKey(
                    question.getMatrixRowId().toString(),
                    question.getCustomLearningLevelId().toString(),
                    type);
            MatrixRequirement requirement = expectedRequirements.get(key);
            if (requirement == null) {
                throw new IllegalArgumentException(
                        "Generated question references an unknown Matrix allocation");
            }
            if (!requirement.questionType().equals(type)) {
                throw new IllegalArgumentException(
                        "Generated question type does not match the accepted Matrix");
            }
            List<UUID> suppliedIds = question.getLearningOutcomeIds();
            LinkedHashSet<UUID> suppliedSet = new LinkedHashSet<>(suppliedIds);
            if (suppliedIds.size() != suppliedSet.size()
                    || !suppliedSet.equals(requirement.learningOutcomeIds())) {
                throw new IllegalArgumentException(
                        "Generated question learning outcomes do not match the accepted Matrix requirement");
            }
            actualDistribution.merge(key, 1, Integer::sum);
            if (!"NONE".equals(visualType(question))) {
                actualVisualDistribution.merge(key, 1, Integer::sum);
            }
        }
        Map<String, Integer> expectedDistribution = new HashMap<>();
        expectedRequirements.forEach((key, requirement) ->
                expectedDistribution.put(key, requirement.questionCount()));
        if (!expectedDistribution.equals(actualDistribution)) {
            throw new IllegalArgumentException(
                    "Generated questions do not match the accepted row, level, type, and count distribution");
        }
        Map<String, Integer> expectedVisualDistribution = new HashMap<>();
        expectedRequirements.forEach((key, requirement) ->
                expectedVisualDistribution.put(key, requirement.imageQuestionCount()));
        expectedVisualDistribution.entrySet().removeIf(entry -> entry.getValue() == 0);
        if (!expectedVisualDistribution.equals(actualVisualDistribution)) {
            throw new IllegalArgumentException("Generated visual counts do not match the accepted matrix quotas");
        }
    }

    private void validateDynamicDistribution(
            JsonNode snapshot,
            List<QuizWebhookRequest.QuestionPayload> questions) {
        Map<String, DynamicRequirement> expectedRequirements = new HashMap<>();
        snapshot.path("examMatrix").forEach(row -> row.path("levelCounts").forEach(level -> {
            String rowId = row.path("rowId").asText();
            String levelId = level.path("customLearningLevelId").asText();
            LinkedHashSet<UUID> outcomeIds = new LinkedHashSet<>();
            level.path("learningOutcomeIds").forEach(id -> outcomeIds.add(UUID.fromString(id.asText())));
            expectedRequirements.put(dynamicKey(rowId, levelId), new DynamicRequirement(
                    level.path("questionCount").asInt(), outcomeIds));
        }));

        Map<String, Integer> actualDistribution = new HashMap<>();
        for (QuizWebhookRequest.QuestionPayload question : questions) {
            validateQuestionShape(question);
            if (!"NONE".equals(visualType(question))) {
                throw new IllegalArgumentException("Contract version 2 does not permit generated visuals");
            }
            if (question.getMatrixRowId() == null || question.getCustomLearningLevelId() == null) {
                throw new IllegalArgumentException(
                        "Every generated question must identify its matrix row and custom learning level");
            }
            String key = dynamicKey(
                    question.getMatrixRowId().toString(),
                    question.getCustomLearningLevelId().toString());
            DynamicRequirement requirement = expectedRequirements.get(key);
            if (requirement == null) {
                throw new IllegalArgumentException(
                        "Generated question references an unknown matrix row or custom learning level");
            }
            List<UUID> suppliedIds = question.getLearningOutcomeIds();
            LinkedHashSet<UUID> suppliedSet = new LinkedHashSet<>(suppliedIds);
            if (suppliedIds.size() != suppliedSet.size()
                    || !suppliedSet.equals(requirement.learningOutcomeIds())) {
                throw new IllegalArgumentException(
                        "Generated question learning outcomes do not match the accepted matrix requirement");
            }
            actualDistribution.merge(key, 1, Integer::sum);
        }
        Map<String, Integer> expectedDistribution = new HashMap<>();
        expectedRequirements.forEach((key, requirement) ->
                expectedDistribution.put(key, requirement.questionCount()));
        if (!expectedDistribution.equals(actualDistribution)) {
            throw new IllegalArgumentException(
                    "Generated questions do not match the accepted row and custom-level distribution");
        }
    }

    private void validateLegacyDistribution(
            JsonNode snapshot,
            List<QuizWebhookRequest.QuestionPayload> questions) {
        Map<String, Integer> expectedDistribution = new HashMap<>();
        snapshot.path("examMatrix").forEach(row -> {
            String outcomeId = row.path("learningOutcomeId").asText();
            addExpected(expectedDistribution, outcomeId, "MULTIPLE_CHOICE", row.path("hieu").asInt());
            addExpected(expectedDistribution, outcomeId, "SHORT_ANSWER", row.path("van_dung").asInt());
            addExpected(expectedDistribution, outcomeId, "CODE_RUNNER", row.path("vdc").asInt());
        });
        Map<String, Integer> actualDistribution = new HashMap<>();
        for (QuizWebhookRequest.QuestionPayload question : questions) {
            validateQuestionShape(question);
            if (!"NONE".equals(visualType(question))) {
                throw new IllegalArgumentException("Contract version 1 does not permit generated visuals");
            }
            if (question.getLearningOutcomeIds() == null || question.getLearningOutcomeIds().size() != 1
                    || question.getOriginalType() == null) {
                throw new IllegalArgumentException(
                        "Every generated question must identify one matrix outcome and original type");
            }
            String key = distributionKey(question.getLearningOutcomeIds().get(0).toString(),
                    question.getOriginalType());
            actualDistribution.merge(key, 1, Integer::sum);
        }
        if (!expectedDistribution.equals(actualDistribution)) {
            throw new IllegalArgumentException(
                    "Generated questions do not match the accepted matrix distribution");
        }
    }

    private void validateQuestionShape(QuizWebhookRequest.QuestionPayload question) {
        if (question.getContent() == null || question.getContent().isBlank()) {
            throw new IllegalArgumentException("Generated question content is required");
        }
        if (question.getLearningOutcomeIds() == null || question.getLearningOutcomeIds().isEmpty()
                || question.getOriginalType() == null || question.getOriginalType().isBlank()) {
            throw new IllegalArgumentException(
                    "Every generated question must identify matrix outcomes and original type");
        }
        String originalType = question.getOriginalType().toUpperCase();
        if (!Set.of("MULTIPLE_CHOICE", "SHORT_ANSWER", "CODE_RUNNER").contains(originalType)) {
            throw new IllegalArgumentException("Generated question type is unsupported");
        }
        boolean multipleChoice = "MULTIPLE_CHOICE".equals(originalType);
        Question.QuestionType expectedLmsType = multipleChoice
                ? Question.QuestionType.MULTIPLE_CHOICE
                : "CODE_RUNNER".equals(originalType)
                        ? Question.QuestionType.CODE_RUNNER
                        : Question.QuestionType.ESSAY;
        if (question.getType() != expectedLmsType) {
            throw new IllegalArgumentException("Generated question type conversion is invalid");
        }
        if (multipleChoice && (question.getAnswers() == null || question.getAnswers().size() < 2
                || question.getAnswers().stream()
                        .noneMatch(answer -> Boolean.TRUE.equals(answer.getIsCorrect())))) {
            throw new IllegalArgumentException(
                    "Multiple-choice questions require answer options and a correct answer");
        }
        visualType(question);
    }

    private String visualType(QuizWebhookRequest.QuestionPayload question) {
        String type = question.getVisual() == null || question.getVisual().getType() == null
                ? "NONE" : question.getVisual().getType().trim().toUpperCase();
        if (!Set.of("NONE", "AUTO", "LATEX", "SVG").contains(type)) {
            throw new IllegalArgumentException("AI quiz visuals may only be NONE, AUTO, LATEX, or SVG");
        }
        if (!"NONE".equals(type)) {
            if (question.getVisual().getBrief() == null || question.getVisual().getBrief().isBlank()
                    || question.getVisual().getAltText() == null || question.getVisual().getAltText().isBlank()) {
                throw new IllegalArgumentException("Generated visuals require a brief and alt text");
            }
        }
        return type;
    }

    private static void addExpected(Map<String, Integer> distribution, String outcomeId, String type, int count) {
        if (count > 0) {
            distribution.merge(distributionKey(outcomeId, type), count, Integer::sum);
        }
    }

    private static String distributionKey(String outcomeId, String type) {
        return outcomeId + "|" + type.toUpperCase();
    }

    private static String dynamicKey(String rowId, String levelId) {
        return rowId + "|" + levelId;
    }

    private static String matrixKey(String rowId, String levelId, String questionType) {
        return rowId + "|" + levelId + "|" + questionType.toUpperCase();
    }

    private record MatrixRequirement(
            int questionCount,
            int imageQuestionCount,
            String questionType,
            LinkedHashSet<UUID> learningOutcomeIds) {
    }

    private record CcctRequirement(
            int questionCount,
            UUID ccctItemId,
            String questionType,
            LinkedHashSet<UUID> learningOutcomeIds) {
    }

    private record DynamicRequirement(int questionCount, LinkedHashSet<UUID> learningOutcomeIds) {
    }

    private Quiz persistQuiz(QuizGenerationJob job, QuizWebhookRequest.QuizPayload payload) {
        Quiz quiz = Quiz.builder()
                .quizTitle(job.getQuizTitle() == null || job.getQuizTitle().isBlank()
                        ? payload.getQuizTitle() : job.getQuizTitle())
                .description(job.getDescription())
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
                .lifecycleStatus(QuizLifecycleStatus.REVIEW_PENDING)
                .sourceType(QuizSourceType.AI_GENERATED)
                .build();
        Quiz savedQuiz = saveQuizPort.save(quiz);
        LinkedHashSet<UUID> quizOutcomeIds = new LinkedHashSet<>();

        for (QuizWebhookRequest.QuestionPayload item : payload.getQuestions()) {
            List<UUID> outcomeIds = item.getLearningOutcomeIds() == null ? List.of() : item.getLearningOutcomeIds();
            if (!learningOutcomesPort.allBelongToCourse(job.getCourseId(), outcomeIds)) {
                throw new IllegalArgumentException("Learning outcome is outside the job course");
            }
            String generatedVisualType = visualType(item);
            UUID generatedImageId = "NONE".equals(generatedVisualType) ? null : UUID.randomUUID();
            Question savedQuestion = saveQuestionPort.save(Question.builder()
                    .content(item.getContent())
                    .type(item.getType() == null ? Question.QuestionType.ESSAY : item.getType())
                    .images(generatedImageId == null ? List.of() : List.of(Question.Image.builder()
                            .id(generatedImageId)
                            .sourceType(Question.Image.SourceType.valueOf(generatedVisualType))
                            .generationBrief(item.getVisual().getBrief())
                            .altText(item.getVisual().getAltText())
                            .status(Question.Image.Status.PENDING)
                            .build()))
                    .reviewStatus(QuestionReviewStatus.PENDING_REVIEW)
                    .sourceType(QuestionSourceType.AI_GENERATED)
                    .quizId(savedQuiz.getId()).build());
            if (generatedImageId != null) {
                visualJobs.create(job.getId(), generatedImageId, generatedVisualType);
            }
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
        Runnable notify = () -> {
            try {
                schedulerPort.unschedule(job.getId());
            } catch (RuntimeException ignored) {
            }
            sseService.pushStatus(job.getId(), response);
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notify.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notify.run();
            }
        });
    }
}
