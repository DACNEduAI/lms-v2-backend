package edu.ai.lms.core.coderunner.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CodeRunnerContractPersistenceAdapterIT extends IntegrationTestConfiguration {

    @Autowired
    private CodeRunnerContractPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void persistsAndReloadsPrivateAndPublicContractDataFromPostgreSql() {
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        seedQuestion(teacherId, courseId, quizId, questionId);

        ExecutableAssessmentContract contract = contract();
        UUID configId = adapter.saveRevision(questionId, contract);
        ExecutableAssessmentContract reloaded = adapter.findLatestByQuestionId(questionId).orElseThrow();

        assertThat(configId).isNotNull();
        assertThat(reloaded).isEqualTo(contract);
        assertThat(reloaded.runtime().executionLimits())
                .isEqualTo(new ExecutableAssessmentContract.ExecutionLimits(5_000, 131_072));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM edu.code_runner_configs WHERE question_id = ?",
                Integer.class,
                questionId)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT wall_time_limit_ms FROM edu.code_runner_configs WHERE question_id = ?",
                Integer.class,
                questionId)).isEqualTo(5_000);
        assertThat(jdbc.queryForObject(
                "SELECT memory_limit_kb FROM edu.code_runner_configs WHERE question_id = ?",
                Integer.class,
                questionId)).isEqualTo(131_072);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM edu.code_runner_test_cases WHERE code_runner_config_id = ?",
                Integer.class,
                configId)).isEqualTo(2);
    }

    @Test
    void createsASecondImmutableRevisionInsteadOfOverwritingTheFirst() {
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        seedQuestion(teacherId, courseId, quizId, questionId);

        adapter.saveRevision(questionId, contract());
        ExecutableAssessmentContract revised = new ExecutableAssessmentContract(
                "2.0.0",
                "is_even_v1",
                "1.0.1",
                "READY_FOR_REVIEW",
                "Even number revised",
                "Return true when the integer is even.",
                contract().runtime(),
                contract().privateGrading());
        adapter.saveRevision(questionId, revised);

        assertThat(adapter.findLatestByQuestionId(questionId).orElseThrow().contractVersion())
                .isEqualTo("1.0.1");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM edu.code_runner_configs WHERE question_id = ?",
                Integer.class,
                questionId)).isEqualTo(2);
    }

    @Test
    void onlyMarksTheLatestRevisionAndAllowsItsReverification() {
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        seedQuestion(teacherId, courseId, quizId, questionId);

        UUID firstConfigId = adapter.saveRevision(questionId, contract());
        UUID latestConfigId = adapter.saveRevision(questionId, contract());

        assertThat(adapter.markVerified(questionId, firstConfigId)).isFalse();
        assertThat(adapter.markVerified(questionId, latestConfigId)).isTrue();
        assertThat(adapter.markVerified(questionId, latestConfigId)).isTrue();
        assertThat(adapter.findLatestByQuestionId(questionId).orElseThrow().status())
                .isEqualTo("VERIFIED_EXECUTION_GOLD");
    }

    private void seedQuestion(UUID teacherId, UUID courseId, UUID quizId, UUID questionId) {
        jdbc.update("INSERT INTO edu.users (id, email, password, role) VALUES (?, ?, 'secret', 'TEACHER')",
                teacherId,
                teacherId + "@test.local");
        jdbc.update("INSERT INTO edu.teachers (user_id, employee_id) VALUES (?, ?)",
                teacherId,
                "GV-" + teacherId);
        jdbc.update("""
                INSERT INTO edu.courses (id, course_name, course_code, active, start_date, end_date)
                VALUES (?, 'DSA', ?, true, NOW(), NOW() + INTERVAL '90 days')
                """, courseId, "DSA-" + courseId);
        jdbc.update("""
                INSERT INTO edu.quizzes (id, quiz_title, course_id, teacher_id, lifecycle_status)
                VALUES (?, 'Code Runner Quiz', ?, ?, 'DRAFT')
                """, quizId, courseId, teacherId);
        jdbc.update("""
                INSERT INTO edu.questions (id, content, type, quiz_id, review_status, source_type)
                VALUES (?, 'Implement isEven', 'CODE_RUNNER', ?, 'APPROVED', 'MANUAL')
                """, questionId, quizId);
    }

    private ExecutableAssessmentContract contract() {
        var integer = new ExecutableAssessmentContract.VersionedRef("integer", "1.0.0");
        var bool = new ExecutableAssessmentContract.VersionedRef("boolean", "1.0.0");
        var signature = new ExecutableAssessmentContract.FunctionSignature(
                bool,
                List.of(new ExecutableAssessmentContract.ArgumentSpec("value", integer, Map.of())));

        Map<String, com.fasterxml.jackson.databind.JsonNode> publicArguments = new LinkedHashMap<>();
        publicArguments.put("value", objectMapper.valueToTree(2));
        Map<String, com.fasterxml.jackson.databind.JsonNode> hiddenArguments = new LinkedHashMap<>();
        hiddenArguments.put("value", objectMapper.valueToTree(-3));

        var publicTest = new ExecutableAssessmentContract.TestCase(
                "public_001", "example", "PUBLIC", publicArguments,
                objectMapper.valueToTree(true), 0);
        var hiddenTest = new ExecutableAssessmentContract.TestCase(
                "hidden_001", "negative", "HIDDEN", hiddenArguments,
                objectMapper.valueToTree(false), 1);
        var runtime = new ExecutableAssessmentContract.RuntimeContract(
                new ExecutableAssessmentContract.VersionedRef("dsa_cpp17", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("scalar_function", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("exact_typed", "1.0.0"),
                "isEven",
                signature,
                "bool isEven(int value) { return false; }",
                List.of(publicTest),
                List.of("\\bsystem\\s*\\("),
                new ExecutableAssessmentContract.ExecutionLimits(5_000, 131_072));
        return new ExecutableAssessmentContract(
                "2.0.0",
                "is_even_v1",
                "1.0.0",
                "READY_FOR_REVIEW",
                "Even number",
                "Return true when the integer is even.",
                runtime,
                new ExecutableAssessmentContract.PrivateGrading(
                        "bool isEven(int value) { return value % 2 == 0; }",
                        List.of(hiddenTest)));
    }
}
