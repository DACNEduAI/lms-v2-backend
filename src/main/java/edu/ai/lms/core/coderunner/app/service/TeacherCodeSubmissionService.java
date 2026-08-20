package edu.ai.lms.core.coderunner.app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.coderunner.app.domain.CodeRunnerActor;
import edu.ai.lms.core.coderunner.app.domain.CodeRunnerQuestionContext;
import edu.ai.lms.core.coderunner.app.domain.TeacherCodeSubmissionView;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerAccessDeniedException;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerConflictException;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerNotFoundException;
import edu.ai.lms.core.coderunner.app.port.out.CheckCodeRunnerCourseAccessPort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerActorPort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerQuestionContextPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherCodeSubmissionService {
    private final LoadCodeRunnerActorPort actorPort;
    private final LoadCodeRunnerQuestionContextPort questionContextPort;
    private final CheckCodeRunnerCourseAccessPort courseAccessPort;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<TeacherCodeSubmissionView> list(UUID courseId, UUID quizId, UUID questionId) {
        requireTeacherAccess(courseId, quizId, questionId);
        return jdbc.query("""
                SELECT s.id,s.attempt_id,s.student_id,u.email,s.status,s.verdict,s.score,s.max_score,
                       s.attempt_count,s.accepted_at,s.completed_at,
                       EXISTS(SELECT 1 FROM edu.attempt_code_submission_snapshots x
                              WHERE x.attempt_id=s.attempt_id AND x.submission_id=s.id)
                FROM edu.code_submissions s JOIN edu.users u ON u.id=s.student_id
                WHERE s.question_id=? AND s.submission_mode='SUBMIT'
                ORDER BY s.accepted_at DESC LIMIT 200
                """, (rs, row) -> new TeacherCodeSubmissionView(
                rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getObject(3, UUID.class),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getDouble(7), rs.getDouble(8),
                rs.getInt(9), rs.getTimestamp(10).toInstant(),
                rs.getTimestamp(11) == null ? null : rs.getTimestamp(11).toInstant(), rs.getBoolean(12)),
                questionId);
    }

    @Transactional
    public TeacherCodeSubmissionView regrade(
            UUID courseId, UUID quizId, UUID questionId, UUID submissionId, String reason) {
        CodeRunnerActor actor = requireTeacherAccess(courseId, quizId, questionId);
        RegradeTarget target = jdbc.query("""
                SELECT s.attempt_id,s.status,a.status,a.score
                FROM edu.code_submissions s JOIN edu.attempts a ON a.id=s.attempt_id
                WHERE s.id=? AND s.question_id=? AND s.submission_mode='SUBMIT'
                  AND EXISTS(SELECT 1 FROM edu.attempt_code_submission_snapshots x
                             WHERE x.attempt_id=s.attempt_id AND x.submission_id=s.id)
                FOR UPDATE OF s,a
                """, rs -> rs.next() ? new RegradeTarget(rs.getObject(1, UUID.class), rs.getString(2),
                rs.getString(3), rs.getDouble(4)) : null, submissionId, questionId);
        if (target == null) throw new CodeRunnerNotFoundException("Selected Code Runner submission not found");
        if (!List.of("COMPLETED", "FAILED_SYSTEM").contains(target.submissionStatus())) {
            throw new CodeRunnerConflictException("Only a completed or failed-system submission can be regraded");
        }
        if (!List.of("FINISHED", "FINALIZING").contains(target.attemptStatus())) {
            throw new CodeRunnerConflictException("The attempt is not ready for regrading");
        }

        Integer gradable = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT q.id) FROM edu.questions q
                WHERE q.quiz_id=? AND q.type IN('MULTIPLE_CHOICE','TRUE_FALSE','CODE_RUNNER')
                """, Integer.class, quizId);
        Double currentCodeContribution = jdbc.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN s.status='COMPLETED' AND s.max_score>0
                    THEN s.score/s.max_score ELSE 0 END),0)
                FROM edu.attempt_code_submission_snapshots x
                JOIN edu.code_submissions s ON s.id=x.submission_id
                WHERE x.attempt_id=?
                """, Double.class, target.attemptId());
        double contribution = gradable == null || gradable == 0 ? 0
                : (currentCodeContribution == null ? 0 : currentCodeContribution) / gradable * 10.0;
        double baseScore = Math.max(0, Math.round((target.currentScore() - contribution) * 100.0) / 100.0);

        jdbc.update("DELETE FROM edu.code_submission_test_results WHERE submission_id=?", submissionId);
        jdbc.update("""
                UPDATE edu.code_submissions SET status='PENDING',verdict=NULL,score=0,attempt_count=0,
                    next_attempt_at=CURRENT_TIMESTAMP,worker_id=NULL,lease_until=NULL,completed_at=NULL
                WHERE id=?
                """, submissionId);
        jdbc.update("UPDATE edu.attempts SET status='FINALIZING',score=?,error_reason=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                baseScore, target.attemptId());
        jdbc.update("""
                INSERT INTO edu.code_runner_submission_actions
                    (submission_id,attempt_id,question_id,actor_id,action,previous_status,reason)
                VALUES (?,?,?,?, 'REGRADE', ?, ?)
                """, submissionId, target.attemptId(), questionId, actor.userId(), target.submissionStatus(),
                auditReason(reason));
        return list(courseId, quizId, questionId).stream()
                .filter(item -> item.submissionId().equals(submissionId)).findFirst()
                .orElseThrow(() -> new CodeRunnerNotFoundException("Requeued submission could not be loaded"));
    }

    private CodeRunnerActor requireTeacherAccess(UUID courseId, UUID quizId, UUID questionId) {
        CodeRunnerActor actor = actorPort.loadCurrentActor()
                .orElseThrow(() -> new CodeRunnerAccessDeniedException("User is not authenticated"));
        if (actor.role() != Roles.TEACHER) {
            throw new CodeRunnerAccessDeniedException("Only a course teacher can inspect Code Runner submissions");
        }
        CodeRunnerQuestionContext context = questionContextPort.findQuestionContext(questionId)
                .orElseThrow(() -> new CodeRunnerNotFoundException("Question not found"));
        if (!courseId.equals(context.courseId()) || !quizId.equals(context.quizId())
                || !"CODE_RUNNER".equals(context.questionType())) {
            throw new CodeRunnerNotFoundException("Code Runner question not found in this course and quiz");
        }
        if (!courseAccessPort.isTeacherInCourse(actor.userId(), courseId)) {
            throw new CodeRunnerAccessDeniedException("Teacher is not assigned to this course");
        }
        return actor;
    }

    private String auditReason(String reason) {
        if (reason == null || reason.isBlank()) return null;
        String value = reason.trim();
        return value.substring(0, Math.min(value.length(), 500));
    }

    private record RegradeTarget(UUID attemptId, String submissionStatus, String attemptStatus, double currentScore) { }
}
