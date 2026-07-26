package edu.ai.lms.core.quiz.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.domain.ExamMatrixRow;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
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
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuizGenerationJobPort;
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
    private final LoadExamMatrixPort examMatrixPort;
    private final LoadLearningOutcomePort learningOutcomePort;
    private final LoadGroupLearningOutcomePort groupLearningOutcomePort;
    private final LoadChapterPort chapterPort;
    private final GetMaterialPort materialPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public QuizGenerationJobResponse generateQuiz(GenerateQuizCommand command) {
        UserAuth user = signedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));
        if (user.getRole() != Roles.TEACHER) {
            throw new UserNotAuthorizedException("Only teachers can generate official quizzes");
        }
        verifyTeacherMembership(user.getId(), command.courseId());

        String idempotencyKey = normalizeIdempotencyKey(command.idempotencyKey());
        if (idempotencyKey != null && idempotencyKey.length() > 255) {
            throw new InvalidQuizGenerationRequestException("Idempotency-Key must not exceed 255 characters");
        }
        ExamMatrix matrix = examMatrixPort.findById(command.examMatrixId())
                .orElseThrow(() -> new InvalidQuizGenerationRequestException("Exam matrix not found"));
        if (!command.courseId().equals(matrix.getCourseId())) {
            throw new InvalidQuizGenerationRequestException("Exam matrix does not belong to the course");
        }
        int questionCount = matrix.getRows().stream().mapToInt(this::rowQuestionCount).sum();
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
        List<Map<String, Object>> rows = matrix.getRows().stream().map(row -> toRowSnapshot(command.courseId(), row)).toList();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("courseId", command.courseId());
        snapshot.put("examMatrixId", matrix.getId());
        snapshot.put("matrixTitle", matrix.getTitle());
        snapshot.put("examMatrix", rows);
        snapshot.put("selectedMaterials", materialSnapshot);
        snapshot.put("questionCount", questionCount);
        snapshot.put("quizTitle", command.quizTitle());
        snapshot.put("teacherInstruction", command.prompt());
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
                .examMatrixId(matrix.getId())
                .requestedBy(user.getId())
                .requestedRole(user.getRole())
                .quizTitle(command.quizTitle())
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
        UserAuth user = signedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));
        QuizGenerationJob job = loadJobPort.getById(jobId)
                .orElseThrow(() -> new QuizGenerationJobNotFoundException("Quiz generation job not found"));
        if (!job.getRequestedBy().equals(user.getId())) {
            throw new UserNotAuthorizedException("User is not allowed to access this job");
        }
        return toStatusResponse(job);
    }

    private Map<String, Object> toRowSnapshot(UUID courseId, ExamMatrixRow row) {
        LearningOutcome outcome = learningOutcomePort.findById(row.getLearningOutcomeId())
                .filter(value -> courseId.equals(value.getCourseId()))
                .orElseThrow(() -> new InvalidQuizGenerationRequestException("Matrix learning outcome is invalid"));
        String chapter = "";
        if (row.getChapterId() != null) {
            Chapter resolvedChapter = chapterPort.findByKey(courseId, row.getChapterId())
                    .orElseThrow(() -> new InvalidQuizGenerationRequestException("Matrix chapter is invalid"));
            chapter = resolvedChapter.getTitle();
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("rowId", row.getId());
        snapshot.put("learningOutcomeId", outcome.getId());
        snapshot.put("clo", formatLearningOutcomeCode(courseId, outcome));
        snapshot.put("chapter", chapter == null ? "" : chapter);
        snapshot.put("topic", row.getTopic() == null ? "" : row.getTopic());
        snapshot.put("hieu", row.getUnderstandCount());
        snapshot.put("van_dung", row.getApplyCount());
        snapshot.put("vdc", row.getApplyPlusCount());
        return snapshot;
    }

    private String formatLearningOutcomeCode(UUID courseId, LearningOutcome outcome) {
        String outcomeCode = outcome.getCode();
        if (outcome.getGroupLearningOutcomeId() == null) {
            return outcomeCode;
        }
        GroupLearningOutcome group = groupLearningOutcomePort.findById(outcome.getGroupLearningOutcomeId())
                .filter(value -> courseId.equals(value.getCourseId()))
                .orElseThrow(() -> new InvalidQuizGenerationRequestException("Matrix learning outcome group is invalid"));
        String groupCode = group.getCode();
        if (groupCode == null || groupCode.isBlank()) {
            return outcomeCode;
        }
        if (outcomeCode == null || outcomeCode.isBlank()) {
            return groupCode;
        }
        return groupCode.endsWith(".") ? groupCode + outcomeCode : groupCode + "." + outcomeCode;
    }

    private int rowQuestionCount(ExamMatrixRow row) {
        return row.getUnderstandCount() + row.getApplyCount() + row.getApplyPlusCount();
    }

    private void validateSelectedMaterials(UUID courseId, List<UUID> materialIds) {
        for (UUID materialId : materialIds) {
            Material material = materialPort.findByIdAndCourseId(materialId, courseId)
                    .orElseThrow(() -> new InvalidQuizGenerationRequestException(
                            "Selected material does not belong to this course: " + materialId));
            if (material.getType() != Material.MaterialType.PDF) {
                throw new InvalidQuizGenerationRequestException(
                        "Selected material is not an indexable PDF: " + materialId);
            }
            if (material.getStatus() != Material.MaterialStatus.UPLOADED
                    || material.getIngestionStatus() != Material.IngestionStatus.READY) {
                String status = material.getIngestionStatus() == null
                        ? "NOT_STARTED" : material.getIngestionStatus().name();
                throw new InvalidQuizGenerationRequestException(
                        "Selected material is not ready for quiz generation: " + materialId + " (" + status + ")");
            }
        }
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
                .errorCode(job.getErrorCode()).errorMessage(job.getErrorMessage())
                .attemptCount(job.getAttemptCount()).createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt()).build();
    }
}
