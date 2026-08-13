package edu.ai.lms.core.quiz.app.service.genquiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationJobResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.exception.InvalidQuizGenerationRequestException;
import edu.ai.lms.core.quiz.app.exception.QuizGenerationJobNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthorizedException;
import edu.ai.lms.core.quiz.app.port.in.command.GenerateQuizCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.GenerateQuizUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuizGenerationJobUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationContextPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.QuizGenerationContextView;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.ValidateMaterialSelectionPort;
import edu.ai.lms.core.quiz.app.port.out.QuestionVisualJobPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizGenerationService implements GenerateQuizUseCase, GetQuizGenerationJobUseCase {

    private final SaveQuizGenerationJobPort saveJobPort;
    private final LoadQuizGenerationJobPort loadJobPort;
    private final ScheduleQuizGenerationJobPort schedulerPort;
    private final LoadSignedInUserPort signedInUserPort;
    private final LoadCourseMembershipPort membershipPort;
    private final LoadQuizGenerationContextPort generationContextPort;
    private final ValidateMaterialSelectionPort materialSelectionPort;
    private final ObjectMapper objectMapper;
    private final QuestionVisualJobPort visualJobs;

    @Override
    @Transactional
    public QuizGenerationJobResponse generateQuiz(GenerateQuizCommand command) {
        CurrentActor user = signedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));
        if (user.getRole() != Roles.TEACHER) {
            throw new UserNotAuthorizedException("Only teachers can generate official quizzes");
        }
        verifyTeacherMembership(user.getId(), command.courseId());

        String idempotencyKey = normalizeIdempotencyKey(command.idempotencyKey());
        if (idempotencyKey != null && idempotencyKey.length() > 255) {
            throw new InvalidQuizGenerationRequestException("Idempotency-Key must not exceed 255 characters");
        }
        QuizGenerationContextView context = generationContextPort.load(command.courseId(), command.examMatrixId())
                .orElseThrow(() -> new InvalidQuizGenerationRequestException("Exam matrix not found"));
        int questionCount = context.rows().stream().mapToInt(QuizGenerationContextView.Row::questionCount).sum();
        if (questionCount <= 0) {
            throw new InvalidQuizGenerationRequestException("Exam matrix must request at least one question");
        }

        List<UUID> materialIds = command.selectedMaterialIds() == null ? List.of() : command.selectedMaterialIds();
        materialIds = materialIds.stream().distinct().toList();
        validateSelectedMaterials(command.courseId(), materialIds);
        List<Map<String, Object>> materialSnapshot = materialIds.stream().distinct().map(id -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("materialId", id);
            return item;
        }).toList();

        validateTiming(command);
        GenerateQuizCommand.QuestionDefaultsConfiguration questionDefaultsConfiguration =
                normalizeQuestionDefaultsConfiguration(command.questionDefaultsConfiguration());
        Map<String, Integer> visualCounts = validateVisualRequirements(context, command.visualRequirements());
        Map<String, String> questionTypes = validateQuestionTypeOverrides(
                context, command.questionTypeOverrides());
        List<Map<String, Object>> rows = context.rows().stream()
                .map(row -> toRowSnapshot(row, visualCounts, questionTypes)).toList();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("snapshotVersion", 4);
        snapshot.put("courseId", command.courseId());
        snapshot.put("courseTitle", context.courseTitle());
        snapshot.put("examMatrixId", context.examMatrixId());
        snapshot.put("matrixTitle", context.matrixTitle());
        snapshot.put("examMatrix", rows);
        snapshot.put("selectedMaterials", materialSnapshot);
        snapshot.put("questionCount", questionCount);
        snapshot.put("quizTitle", command.quizTitle());
        snapshot.put("description", command.description());
        snapshot.put("teacherInstruction", command.prompt());
        snapshot.put("questionDefaultsConfiguration", questionDefaultsConfiguration);
        snapshot.put("strictMode", command.strictMode() == null || command.strictMode());
        snapshot.put("codeRunnerPolicy", command.codeRunnerPolicy() == null
                ? "PRESERVE" : command.codeRunnerPolicy());
        snapshot.put("unlimited", command.unlimited());
        snapshot.put("timeLimitMinutes", command.timeLimitMinutes());
        snapshot.put("passScore", command.passScore());
        snapshot.put("startTime", command.startTime());
        snapshot.put("dueTime", command.dueTime());
        snapshot.put("attemptLimited", command.attemptLimited());
        String snapshotJson = writeJson(snapshot);
        String payloadHash = sha256(snapshotJson);
        if (idempotencyKey != null) {
            var existing = loadJobPort.findByIdempotencyKey(user.getId(), idempotencyKey);
            if (existing.isPresent()) {
                if (!payloadHash.equals(existing.get().getPayloadHash())) {
                    throw new InvalidQuizGenerationRequestException(
                            "Idempotency-Key was already used for a different generation request");
                }
                return toAcceptedResponse(existing.get());
            }
        }

        QuizGenerationJob job = QuizGenerationJob.builder()
                .courseId(command.courseId())
                .examMatrixId(context.examMatrixId())
                .requestedBy(user.getId())
                .requestedRole(user.getRole())
                .quizTitle(command.quizTitle())
                .description(command.description())
                .prompt(command.prompt())
                .requestSnapshot(snapshotJson)
                .payloadHash(payloadHash)
                .idempotencyKey(idempotencyKey)
                .strictMode(command.strictMode() == null || command.strictMode())
                .codeRunnerPolicy(command.codeRunnerPolicy() == null ? "PRESERVE" : command.codeRunnerPolicy())
                .unlimited(command.unlimited())
                .timeLimitMinutes(command.timeLimitMinutes())
                .passScore(command.passScore())
                .startTime(command.startTime())
                .dueTime(command.dueTime())
                .attemptLimited(command.attemptLimited())
                .display(false)
                .status(QuizGenerationStatus.PENDING)
                .phase(QuizGenerationJob.Phase.TEXT_GENERATION)
                .attemptCount(0)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        QuizGenerationJob saved = saveJobPort.save(job);
        if (!payloadHash.equals(saved.getPayloadHash())) {
            throw new InvalidQuizGenerationRequestException(
                    "Idempotency-Key was already used for a different generation request");
        }
        if (!job.getId().equals(saved.getId())) {
            return toAcceptedResponse(saved);
        }
        scheduleAfterCommit(saved);
        return toAcceptedResponse(saved);
    }

    private void scheduleAfterCommit(QuizGenerationJob job) {
        Runnable schedule = () -> {
            try {
                schedulerPort.schedule(job.getId(), job.getNextAttemptAt());
            } catch (RuntimeException ignored) {
                // The durable PENDING row is recovered by QuizGenerationRecoveryJob.
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    schedule.run();
                }
            });
            return;
        }
        schedule.run();
    }

    @Override
    public QuizGenerationStatusResponse getStatus(UUID jobId) {
        CurrentActor user = signedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));
        QuizGenerationJob job = loadJobPort.getById(jobId)
                .orElseThrow(() -> new QuizGenerationJobNotFoundException("Quiz generation job not found"));
        if (!job.getRequestedBy().equals(user.getId())) {
            throw new UserNotAuthorizedException("User is not allowed to access this job");
        }
        QuizGenerationStatusResponse response = toStatusResponse(job);
        QuestionVisualJobPort.Summary summary = visualJobs.summarize(job.getId());
        response.setVisualSummary(new QuizGenerationStatusResponse.VisualSummary(
                summary.requested(), summary.ready(), summary.failed()));
        return response;
    }

    private Map<String, Object> toRowSnapshot(
            QuizGenerationContextView.Row row,
            Map<String, Integer> visualCounts,
            Map<String, String> questionTypes) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("rowId", row.rowId());
        snapshot.put("chapterId", row.chapterId());
        snapshot.put("chapter", row.chapterTitle());
        snapshot.put("content", row.topic());
        snapshot.put("topic", row.topic());
        snapshot.put("learningOutcomes", row.learningOutcomes().stream().map(outcome -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", outcome.id());
            value.put("code", outcome.code());
            value.put("description", outcome.description());
            return value;
        }).toList());
        snapshot.put("levelCounts", row.levelCounts().stream().map(level -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("customLearningLevelId", level.customLearningLevelId());
            value.put("code", level.code());
            value.put("name", level.name());
            value.put("description", level.description());
            value.put("sortOrder", level.sortOrder());
            value.put("learningOutcomeIds", level.learningOutcomeIds());
            value.put("questionType", questionTypes.getOrDefault(
                    groupKey(row.rowId(), level.customLearningLevelId()), level.questionType()));
            value.put("questionCount", level.questionCount());
            value.put("imageQuestionCount", visualCounts.getOrDefault(
                    groupKey(row.rowId(), level.customLearningLevelId()), 0));
            return value;
        }).toList());
        return snapshot;
    }

    private Map<String, Integer> validateVisualRequirements(
            QuizGenerationContextView context,
            List<GenerateQuizCommand.VisualRequirement> requirements) {
        Map<String, Integer> available = new LinkedHashMap<>();
        context.rows().forEach(row -> row.levelCounts().forEach(level ->
                available.put(groupKey(row.rowId(), level.customLearningLevelId()), level.questionCount())));
        Map<String, Integer> result = new LinkedHashMap<>();
        for (GenerateQuizCommand.VisualRequirement requirement :
                requirements == null ? List.<GenerateQuizCommand.VisualRequirement>of() : requirements) {
            if (requirement == null || requirement.examMatrixRowId() == null
                    || requirement.customLearningLevelId() == null) {
                throw new InvalidQuizGenerationRequestException("Visual requirement group identifiers are required");
            }
            String key = groupKey(requirement.examMatrixRowId(), requirement.customLearningLevelId());
            Integer questionCount = available.get(key);
            if (questionCount == null) {
                throw new InvalidQuizGenerationRequestException(
                        "Visual requirement references a group outside the active exam matrix");
            }
            if (requirement.imageQuestionCount() < 0 || requirement.imageQuestionCount() > questionCount) {
                throw new InvalidQuizGenerationRequestException(
                        "Image question count must be between zero and the group's question count");
            }
            if (result.putIfAbsent(key, requirement.imageQuestionCount()) != null) {
                throw new InvalidQuizGenerationRequestException("Duplicate visual requirement group");
            }
        }
        return result;
    }

    private Map<String, String> validateQuestionTypeOverrides(
            QuizGenerationContextView context,
            List<GenerateQuizCommand.QuestionTypeOverride> overrides) {
        java.util.Set<String> available = new java.util.HashSet<>();
        context.rows().forEach(row -> row.levelCounts().forEach(level ->
                available.add(groupKey(row.rowId(), level.customLearningLevelId()))));
        Map<String, String> result = new LinkedHashMap<>();
        for (GenerateQuizCommand.QuestionTypeOverride override :
                overrides == null ? List.<GenerateQuizCommand.QuestionTypeOverride>of() : overrides) {
            if (override == null || override.examMatrixRowId() == null
                    || override.customLearningLevelId() == null) {
                throw new InvalidQuizGenerationRequestException(
                        "Question type override group identifiers are required");
            }
            String key = groupKey(override.examMatrixRowId(), override.customLearningLevelId());
            if (!available.contains(key)) {
                throw new InvalidQuizGenerationRequestException(
                        "Question type override references a group outside the active exam matrix");
            }
            String questionType = normalizeGenerationQuestionType(override.questionType());
            if (result.putIfAbsent(key, questionType) != null) {
                throw new InvalidQuizGenerationRequestException("Duplicate question type override group");
            }
        }
        return result;
    }

    private String normalizeGenerationQuestionType(String questionType) {
        if (questionType == null) {
            throw new InvalidQuizGenerationRequestException("Question type is required");
        }
        return switch (questionType.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "MULTIPLE_CHOICE" -> "MULTIPLE_CHOICE";
            case "ESSAY" -> "SHORT_ANSWER";
            case "CODE_RUNNER" -> "CODE_RUNNER";
            default -> throw new InvalidQuizGenerationRequestException(
                    "Question type must be MULTIPLE_CHOICE, ESSAY, or CODE_RUNNER");
        };
    }

    private String groupKey(UUID rowId, UUID levelId) {
        return rowId + "|" + levelId;
    }

    private void validateSelectedMaterials(UUID courseId, List<UUID> materialIds) {
        materialSelectionPort.findValidationError(courseId, materialIds)
                .ifPresent(error -> {
                    throw new InvalidQuizGenerationRequestException(error);
                });
    }

    private void validateTiming(GenerateQuizCommand command) {
        if (command.codeRunnerPolicy() != null && !"PRESERVE".equals(command.codeRunnerPolicy())) {
            throw new InvalidQuizGenerationRequestException("Unsupported code runner policy");
        }
        if (command.passScore() != null && (command.passScore() < 0 || command.passScore() > 100)) {
            throw new InvalidQuizGenerationRequestException("Pass score must be between 0 and 100");
        }
        if (command.attemptLimited() != null && command.attemptLimited() <= 0) {
            throw new InvalidQuizGenerationRequestException("Attempt limit must be greater than 0");
        }
        if (Boolean.TRUE.equals(command.unlimited())) {
            return;
        }
        if (command.startTime() != null && command.dueTime() != null && command.dueTime().isBefore(command.startTime())) {
            throw new InvalidQuizGenerationRequestException("Quiz due time must not be before start time");
        }
    }

    private GenerateQuizCommand.QuestionDefaultsConfiguration normalizeQuestionDefaultsConfiguration(
            GenerateQuizCommand.QuestionDefaultsConfiguration defaults) {
        GenerateQuizCommand.MultipleChoiceDefaults multipleChoice =
                defaults == null ? null : defaults.multipleChoice();
        if (multipleChoice == null) {
            multipleChoice = new GenerateQuizCommand.MultipleChoiceDefaults(4, false, true);
        }
        if (multipleChoice.answerOptionCount() < 4 || multipleChoice.answerOptionCount() > 10) {
            throw new InvalidQuizGenerationRequestException(
                    "Answer option count must be between 4 and 10");
        }
        return new GenerateQuizCommand.QuestionDefaultsConfiguration(multipleChoice);
    }

    private void verifyTeacherMembership(UUID userId, UUID courseId) {
        if (!membershipPort.isTeacherInCourse(userId, courseId)) {
            throw new UserNotAuthorizedException("Teacher is not a member of this course");
        }
    }

    private String normalizeIdempotencyKey(String key) {
        return key == null || key.isBlank() ? null : key.trim();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to snapshot generation request", ex);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private QuizGenerationJobResponse toAcceptedResponse(QuizGenerationJob job) {
        String statusUrl = "/api/jobs/" + job.getId();
        return QuizGenerationJobResponse.builder()
                .jobId(job.getId()).status(job.getStatus()).createdAt(job.getCreatedAt())
                .statusUrl(statusUrl).streamUrl(statusUrl + "/stream").build();
    }

    static QuizGenerationStatusResponse toStatusResponse(QuizGenerationJob job) {
        return QuizGenerationStatusResponse.builder()
                .jobId(job.getId()).status(job.getStatus()).quizId(job.getQuizId())
                .phase(job.getPhase())
                .errorCode(job.getErrorCode()).errorMessage(job.getErrorMessage())
                .attemptCount(job.getAttemptCount()).createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt()).build();
    }
}
