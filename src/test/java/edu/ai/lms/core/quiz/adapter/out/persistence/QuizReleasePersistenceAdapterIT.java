package edu.ai.lms.core.quiz.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.coderunner.adapter.out.persistence.CodeRunnerContractPersistenceAdapter;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.quiz.app.exception.QuizLifecycleConflictException;

class QuizReleasePersistenceAdapterIT extends IntegrationTestConfiguration {
    @Autowired private QuizReleasePersistenceAdapter releases;
    @Autowired private CodeRunnerContractPersistenceAdapter contracts;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void pinsVerifiedRevisionAndAttemptKeepsItWhenANewerDraftExists() {
        Fixture fixture = seed();
        UUID verifiedConfig = contracts.saveRevision(fixture.questionId(), contract("1.0.0"));
        assertThat(contracts.markVerified(fixture.questionId(), verifiedConfig)).isTrue();

        UUID releaseId = releases.publishRelease(fixture.quizId(), 1, fixture.teacherId(),
                java.time.LocalDateTime.now());
        UUID attemptId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.attempts
                    (id, student_id, quiz_id, quiz_release_id, status, start_time)
                VALUES (?, ?, ?, ?, 'STARTED', NOW())
                """, attemptId, fixture.studentId(), fixture.quizId(), releaseId);

        contracts.saveRevision(fixture.questionId(), contract("1.0.1"));

        var released = contracts.findReleasedForAttempt(attemptId, fixture.questionId()).orElseThrow();
        assertThat(released.configId()).isEqualTo(verifiedConfig);
        assertThat(released.contract().contractVersion()).isEqualTo("1.0.0");
        assertThat(contracts.findLatestByQuestionId(fixture.questionId()).orElseThrow().contractVersion())
                .isEqualTo("1.0.1");
    }

    @Test
    void refusesPublishWhenLatestRevisionHasNotBeenVerified() {
        Fixture fixture = seed();
        UUID verifiedConfig = contracts.saveRevision(fixture.questionId(), contract("1.0.0"));
        assertThat(contracts.markVerified(fixture.questionId(), verifiedConfig)).isTrue();
        contracts.saveRevision(fixture.questionId(), contract("1.0.1"));

        assertThatThrownBy(() -> releases.publishRelease(fixture.quizId(), 1, fixture.teacherId(),
                java.time.LocalDateTime.now()))
                .isInstanceOf(QuizLifecycleConflictException.class)
                .hasMessageContaining("verified configuration");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM edu.quiz_releases WHERE quiz_id = ?",
                Integer.class, fixture.quizId())).isZero();
    }

    private Fixture seed() {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.users (id,email,password,role) VALUES (?,?,'x','TEACHER')",
                teacherId, teacherId + "@test.local");
        jdbc.update("INSERT INTO edu.teachers (user_id,employee_id) VALUES (?,?)",
                teacherId, "GV-" + teacherId);
        jdbc.update("INSERT INTO edu.users (id,email,password,role) VALUES (?,?,'x','STUDENT')",
                studentId, studentId + "@test.local");
        jdbc.update("INSERT INTO edu.students (user_id,student_id,enrollment_year) VALUES (?,?,2026)",
                studentId, "SV-" + studentId);
        jdbc.update("""
                INSERT INTO edu.courses (id,course_name,course_code,active,start_date,end_date)
                VALUES (?,'DSA',?,true,NOW(),NOW()+INTERVAL '30 days')
                """, courseId, "DSA-" + courseId);
        jdbc.update("""
                INSERT INTO edu.quizzes (id,quiz_title,course_id,teacher_id,lifecycle_status)
                VALUES (?,'Runner',?,?,'DRAFT')
                """, quizId, courseId, teacherId);
        jdbc.update("""
                INSERT INTO edu.questions (id,content,type,quiz_id,review_status,source_type)
                VALUES (?,'isEven','CODE_RUNNER',?,'APPROVED','MANUAL')
                """, questionId, quizId);
        return new Fixture(teacherId, studentId, quizId, questionId);
    }

    private ExecutableAssessmentContract contract(String version) {
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
        return new ExecutableAssessmentContract("2.0.0", "release_pin", version,
                "READY_FOR_REVIEW", "Even number", "Return true if even.", runtime,
                new ExecutableAssessmentContract.PrivateGrading(
                        "bool isEven(int value) { return value % 2 == 0; }", List.of(hiddenTest)));
    }

    private record Fixture(UUID teacherId, UUID studentId, UUID quizId, UUID questionId) { }
}
