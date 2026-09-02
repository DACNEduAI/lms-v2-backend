package edu.ai.lms.core.learnermodel;

import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.quiz.app.domain.QuestionLearningOutcomeLink;
import edu.ai.lms.core.quiz.app.port.out.LoadLearningOutcomeAssociationsPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Converts finalized assessment snapshots into immutable learner evidence.
 * This is deliberately called only by the transaction that wins FINISHED.
 */
@Service
@RequiredArgsConstructor
public class LearnerEvidenceFinalizationService {

    private final LoadAttemptPort loadAttemptPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadLearningOutcomeAssociationsPort learningOutcomeAssociationsPort;
    private final JdbcTemplate jdbcTemplate;

    public void publishForFinishedAttempt(UUID attemptId) {
        Attempt attempt = loadAttemptPort.getById(attemptId).orElseThrow();
        if (attempt.getStatus() != Attempt.AttemptStatus.FINISHED) {
            throw new IllegalStateException("Learner evidence requires a FINISHED attempt");
        }
        UUID courseId = loadQuizPort.getById(attempt.getQuizId())
                .orElseThrow(() -> new IllegalStateException("Quiz not found for finalized attempt"))
                .getCourseId();
        Map<UUID, List<UUID>> outcomesByQuestion = learningOutcomeAssociationsPort
                .findByQuizId(attempt.getQuizId()).stream()
                .collect(Collectors.groupingBy(QuestionLearningOutcomeLink::questionId,
                        Collectors.mapping(QuestionLearningOutcomeLink::learningOutcomeId, Collectors.toList())));

        publishMcqEvents(attempt, courseId, outcomesByQuestion);
        publishCodeRunnerEvents(attempt, courseId, outcomesByQuestion);
        enqueueRebuild(attempt.getStudentId(), courseId);
    }

    private void publishMcqEvents(Attempt attempt, UUID courseId, Map<UUID, List<UUID>> outcomesByQuestion) {
        jdbcTemplate.query("""
                SELECT r.id, r.question_id, r.grading_revision, r.finalized_at,
                       r.is_correct_snapshot, r.score_ratio, q.version
                FROM edu.attempt_question_responses r
                JOIN edu.questions q ON q.id = r.question_id
                WHERE r.attempt_id = ?
                ORDER BY r.finalized_at, r.id
                """, rs -> {
            UUID responseId = rs.getObject("id", UUID.class);
            UUID questionId = rs.getObject("question_id", UUID.class);
            List<UUID> outcomeIds = outcomesByQuestion.getOrDefault(questionId, List.of());
            UUID eventId = insertEvent("MCQ:" + responseId, rs.getInt("grading_revision"), attempt, courseId,
                    questionId, rs.getLong("version"), "MULTIPLE_CHOICE", rs.getTimestamp("finalized_at").toLocalDateTime(),
                    (Boolean) rs.getObject("is_correct_snapshot"), (Double) rs.getObject("score_ratio"), outcomeIds.size() == 1);
            if (eventId != null) snapshotOutcomes(eventId, outcomeIds);
        }, attempt.getId());
    }

    private void publishCodeRunnerEvents(Attempt attempt, UUID courseId, Map<UUID, List<UUID>> outcomesByQuestion) {
        jdbcTemplate.query("""
                SELECT s.question_id, s.submission_id, c.score, c.max_score, c.completed_at, q.version
                FROM edu.attempt_code_submission_snapshots s
                JOIN edu.code_submissions c ON c.id = s.submission_id
                JOIN edu.questions q ON q.id = s.question_id
                WHERE s.attempt_id = ? AND c.status = 'COMPLETED' AND c.max_score > 0
                ORDER BY c.completed_at, s.question_id
                """, rs -> {
            UUID questionId = rs.getObject("question_id", UUID.class);
            UUID submissionId = rs.getObject("submission_id", UUID.class);
            double ratio = rs.getDouble("score") / rs.getDouble("max_score");
            List<UUID> outcomeIds = outcomesByQuestion.getOrDefault(questionId, List.of());
            UUID eventId = insertEvent("CODE:" + attempt.getId() + ":" + questionId + ":" + submissionId, 1,
                    attempt, courseId, questionId, rs.getLong("version"), "CODE_RUNNER",
                    rs.getTimestamp("completed_at").toLocalDateTime(), ratio == 1.0, ratio, outcomeIds.size() == 1);
            if (eventId != null) snapshotOutcomes(eventId, outcomeIds);
        }, attempt.getId());
    }

    private UUID insertEvent(String key, int revision, Attempt attempt, UUID courseId, UUID questionId,
                             long questionVersion, String sourceType, LocalDateTime occurredAt,
                             Boolean correct, Double scoreRatio, boolean hasExactlyOneLearningOutcome) {
        try {
            return jdbcTemplate.queryForObject("""
                    INSERT INTO edu.learning_events
                    (id, observation_key, observation_revision, student_id, course_id, quiz_id, attempt_id,
                     question_id, question_version, source_type, event_type, occurred_at, is_correct, score_ratio,
                     mastery_eligible)
                    VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, 'GRADED_RESPONSE_FINALIZED', ?, ?, ?, ?)
                    ON CONFLICT (observation_key, observation_revision) DO NOTHING
                    RETURNING id
                    """, UUID.class, key, revision, attempt.getStudentId(), courseId, attempt.getQuizId(), attempt.getId(),
                    questionId, questionVersion, sourceType, Timestamp.valueOf(occurredAt), correct, scoreRatio,
                    correct != null && hasExactlyOneLearningOutcome);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private void snapshotOutcomes(UUID eventId, List<UUID> outcomeIds) {
        outcomeIds.forEach(outcomeId -> jdbcTemplate.update("""
                INSERT INTO edu.learning_event_learning_outcomes (learning_event_id, learning_outcome_id)
                VALUES (?, ?) ON CONFLICT DO NOTHING
                """, eventId, outcomeId));
    }

    private void enqueueRebuild(UUID studentId, UUID courseId) {
        jdbcTemplate.update("""
                INSERT INTO edu.learner_model_jobs
                (id, dedupe_key, job_type, student_id, course_id, status, next_attempt_at, created_at, updated_at)
                VALUES (gen_random_uuid(), ?, 'REBUILD_STUDENT_COURSE', ?, ?, 'PENDING', CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (dedupe_key) DO UPDATE SET
                    status = CASE WHEN edu.learner_model_jobs.status IN ('COMPLETED', 'FAILED')
                        THEN 'PENDING' ELSE edu.learner_model_jobs.status END,
                    next_attempt_at = CASE WHEN edu.learner_model_jobs.status IN ('COMPLETED', 'FAILED')
                        THEN CURRENT_TIMESTAMP ELSE edu.learner_model_jobs.next_attempt_at END,
                    completed_at = CASE WHEN edu.learner_model_jobs.status IN ('COMPLETED', 'FAILED')
                        THEN NULL ELSE edu.learner_model_jobs.completed_at END,
                    updated_at = CURRENT_TIMESTAMP
                """, "REBUILD:" + studentId + ":" + courseId, studentId, courseId);
    }
}
