package edu.ai.lms.core.coderunner.adapter.out.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.domain.SubmittedAnswer;
import edu.ai.lms.core.attempt.app.service.AttemptScoringService;
import edu.ai.lms.core.coderunner.adapter.out.persistence.CodeRunnerContractPersistenceAdapter;
import edu.ai.lms.core.coderunner.app.domain.CodeSubmission;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.app.service.StudentCodeRunnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Explicit end-to-end gate: PostgreSQL + project-controlled Judge0 + Attempt score. */
@TestPropertySource(properties = {
        "code-runner.judge0.base-url=http://127.0.0.1:2358",
        "code-runner.judge0.auth-token=lms-local-judge0-token",
        "code-runner.judge0.auth-header=X-Judge0-Token"
})
class StudentCodeRunnerAttemptLiveIT extends IntegrationTestConfiguration {
    @Autowired private StudentCodeRunnerService service;
    @Autowired private CodeRunnerContractPersistenceAdapter contracts;
    @Autowired private AttemptScoringService scoringService;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void persistsRealSubmissionAndContributesToQuizScoreWithoutLeakingHiddenTests() {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        seed(studentId, teacherId, courseId, quizId, questionId, attemptId);
        contracts.saveRevision(questionId, contract());
        contracts.markLatestVerified(questionId);

        var result = service.execute(attemptId, studentId, quizId, questionId,
                "bool isEven(int value) { return value % 2 == 0; }",
                "live-" + UUID.randomUUID(), CodeSubmission.Mode.SUBMIT);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.verdict()).isEqualTo("ACCEPTED");
        assertThat(result.score()).isEqualTo(1);
        assertThat(result.maxScore()).isEqualTo(1);
        assertThat(result.passedTests()).isEqualTo(2);
        assertThat(result.visibleResults()).extracting(item -> item.testId())
                .containsExactly("public_001");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM edu.code_submissions WHERE id=?",
                Integer.class, result.submissionId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM edu.code_submission_test_results WHERE submission_id=?",
                Integer.class, result.submissionId())).isEqualTo(2);

        double quizScore = scoringService.calculateScore(
                List.of(QuestionAnswerKey.builder().questionId(questionId)
                        .questionType(QuestionAnswerKey.QuestionType.CODE_RUNNER).build()),
                List.of(SubmittedAnswer.builder().questionId(questionId)
                        .codeSubmissionId(result.submissionId()).build()), attemptId, studentId);
        assertThat(quizScore).isEqualTo(10.0);
    }

    private void seed(UUID studentId, UUID teacherId, UUID courseId, UUID quizId,
                      UUID questionId, UUID attemptId) {
        jdbc.update("INSERT INTO edu.users (id,email,password,role) VALUES (?,?,'x','STUDENT')",
                studentId, studentId + "@test.local");
        jdbc.update("INSERT INTO edu.students (user_id,student_id,enrollment_year) VALUES (?,?,2026)",
                studentId, "SV-" + studentId);
        jdbc.update("INSERT INTO edu.users (id,email,password,role) VALUES (?,?,'x','TEACHER')",
                teacherId, teacherId + "@test.local");
        jdbc.update("INSERT INTO edu.teachers (user_id,employee_id) VALUES (?,?)",
                teacherId, "GV-" + teacherId);
        jdbc.update("INSERT INTO edu.courses (id,course_name,course_code,active,start_date,end_date) "
                + "VALUES (?,'DSA',?,true,NOW(),NOW()+INTERVAL '30 days')", courseId, "DSA-" + courseId);
        jdbc.update("INSERT INTO edu.quizzes (id,quiz_title,course_id,teacher_id,lifecycle_status) "
                + "VALUES (?,'Runner',?,?,'PUBLISHED')", quizId, courseId, teacherId);
        jdbc.update("INSERT INTO edu.questions (id,content,type,quiz_id,review_status,source_type) "
                + "VALUES (?,'isEven','CODE_RUNNER',?,'APPROVED','MANUAL')", questionId, quizId);
        jdbc.update("INSERT INTO edu.attempts (id,student_id,quiz_id,status,start_time,due_time) "
                + "VALUES (?,?,?,'STARTED',NOW(),NOW()+INTERVAL '30 minutes')", attemptId, studentId, quizId);
    }

    private ExecutableAssessmentContract contract() {
        ObjectMapper mapper = new ObjectMapper();
        var integer = new ExecutableAssessmentContract.VersionedRef("integer", "1.0.0");
        var bool = new ExecutableAssessmentContract.VersionedRef("boolean", "1.0.0");
        var signature = new ExecutableAssessmentContract.FunctionSignature(bool,
                List.of(new ExecutableAssessmentContract.ArgumentSpec("value", integer, Map.of())));
        Map<String, com.fasterxml.jackson.databind.JsonNode> publicArgs = new LinkedHashMap<>();
        publicArgs.put("value", mapper.valueToTree(2));
        Map<String, com.fasterxml.jackson.databind.JsonNode> hiddenArgs = new LinkedHashMap<>();
        hiddenArgs.put("value", mapper.valueToTree(-3));
        var publicTest = new ExecutableAssessmentContract.TestCase("public_001", "positive", "PUBLIC",
                publicArgs, mapper.valueToTree(true), 0);
        var hiddenTest = new ExecutableAssessmentContract.TestCase("hidden_001", "negative", "HIDDEN",
                hiddenArgs, mapper.valueToTree(false), 1);
        var runtime = new ExecutableAssessmentContract.RuntimeContract(
                new ExecutableAssessmentContract.VersionedRef("dsa_cpp17", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("scalar_function", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("exact_typed", "1.0.0"),
                "isEven", signature, "bool isEven(int value) { return false; }",
                List.of(publicTest), List.of("\\bsystem\\s*\\("));
        return new ExecutableAssessmentContract("2.0.0", "is_even_attempt_v1", "1.0.0",
                "READY_FOR_REVIEW", "Even number", "Return true if even.", runtime,
                new ExecutableAssessmentContract.PrivateGrading(
                        "bool isEven(int value) { return value % 2 == 0; }", List.of(hiddenTest)));
    }
}
