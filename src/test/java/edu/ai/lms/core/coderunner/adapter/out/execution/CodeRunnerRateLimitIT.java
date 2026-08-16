package edu.ai.lms.core.coderunner.adapter.out.execution;

import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.coderunner.api.CodeRunnerAcceptedView;
import edu.ai.lms.core.coderunner.app.domain.CodeSubmission;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerRateLimitException;
import edu.ai.lms.core.coderunner.app.service.StudentCodeRunnerService;
import edu.ai.lms.core.coderunner.adapter.out.persistence.CodeRunnerContractPersistenceAdapter;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

class CodeRunnerRateLimitIT extends IntegrationTestConfiguration {

    @Autowired private StudentCodeRunnerService service;
    @Autowired private CodeRunnerContractPersistenceAdapter contracts;
    @Autowired private JdbcTemplate jdbc;
    
    @MockBean private Clock clock;

    private UUID studentId = UUID.randomUUID();
    private UUID attemptId = UUID.randomUUID();
    private UUID quizId = UUID.randomUUID();
    private UUID questionId = UUID.randomUUID();
    private UUID releaseId = UUID.randomUUID();

    private ExecutableAssessmentContract contract(String id) {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var integer = new ExecutableAssessmentContract.VersionedRef("integer", "1.0.0");
        var bool = new ExecutableAssessmentContract.VersionedRef("boolean", "1.0.0");
        var signature = new ExecutableAssessmentContract.FunctionSignature(bool,
                List.of(new ExecutableAssessmentContract.ArgumentSpec("value", integer, java.util.Map.of())));
        java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> publicArgs = new java.util.LinkedHashMap<>();
        publicArgs.put("value", mapper.valueToTree(2));
        var publicTest = new ExecutableAssessmentContract.TestCase("public_" + id, "positive", "PUBLIC",
                publicArgs, mapper.valueToTree(true), 0.0);
        var hiddenTest = new ExecutableAssessmentContract.TestCase("hidden_" + id, "negative", "HIDDEN",
                publicArgs, mapper.valueToTree(false), 10.0);
        var runtime = new ExecutableAssessmentContract.RuntimeContract(
                new ExecutableAssessmentContract.VersionedRef("dsa_cpp17", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("scalar_function", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("exact_typed", "1.0.0"),
                "isEven", signature, "def isEven(value):\n  pass",
                List.of(publicTest), List.of());
        return new ExecutableAssessmentContract("2.0.0", "contract_" + id, "1.0.0",
                "READY_FOR_REVIEW", "Even", "Return true if even.", runtime, new ExecutableAssessmentContract.PrivateGrading(
                        "def isEven(value):\n  return value % 2 == 0", List.of(hiddenTest)));
    }

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(Instant.parse("2026-08-14T10:00:00Z"));
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.users (id,email,password,role) VALUES (?,?,'x','STUDENT')", studentId, studentId + "@test.local");
        jdbc.update("INSERT INTO edu.students (user_id,student_id,enrollment_year) VALUES (?,?,2026)", studentId, "SV-" + studentId);
        jdbc.update("INSERT INTO edu.users (id,email,password,role) VALUES (?,?,'x','TEACHER')", teacherId, teacherId + "@test.local");
        jdbc.update("INSERT INTO edu.teachers (user_id,employee_id) VALUES (?,?)", teacherId, "GV-" + teacherId);
        jdbc.update("INSERT INTO edu.courses (id,course_name,course_code,active,start_date,end_date) VALUES (?,'DSA',?,true,NOW(),NOW()+INTERVAL '30 days')", courseId, "DSA-" + courseId);
        jdbc.update("INSERT INTO edu.quizzes (id,quiz_title,course_id,teacher_id,lifecycle_status) VALUES (?,'Runner',?,?,'PUBLISHED')", quizId, courseId, teacherId);
        jdbc.update("INSERT INTO edu.questions (id,content,type,quiz_id,review_status,source_type) VALUES (?,'isEven','CODE_RUNNER',?,'APPROVED','MANUAL')", questionId, quizId);
        releaseId = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.quiz_releases (id,quiz_id,release_number,quiz_version,active,published_by) VALUES (?,?,1,1,true,?)", releaseId, quizId, teacherId);
        jdbc.update("INSERT INTO edu.attempts (id,student_id,quiz_id,quiz_release_id,status,start_time,due_time) VALUES (?,?,?,?,'STARTED',NOW(),NOW()+INTERVAL '30 minutes')", attemptId, studentId, quizId, releaseId);
        
        UUID configId = contracts.saveRevision(questionId, contract("1"));
        contracts.markVerified(questionId, configId);
        jdbc.update("INSERT INTO edu.quiz_release_code_runner_bindings (quiz_release_id,question_id,code_runner_config_id) VALUES (?,?,?)", releaseId, questionId, configId);
    }

    @Test
    void rateLimit_cooldownBlocksSpamButIdempotencyPasses() {
        // 1. Submit lần 1 -> Thành công (202)
        String ikey1 = UUID.randomUUID().toString();
        CodeRunnerAcceptedView res1 = service.execute(attemptId, studentId, quizId, questionId, "code", ikey1, CodeSubmission.Mode.SUBMIT);
        assertThat(res1.submissionId()).isNotNull();

        // 2. Submit lần 2 (cùng IKey) -> Trả lại kết quả cũ (không ném lỗi 429 dù chưa hết 10s)
        CodeRunnerAcceptedView res2 = service.execute(attemptId, studentId, quizId, questionId, "code", ikey1, CodeSubmission.Mode.SUBMIT);
        assertThat(res2.submissionId()).isEqualTo(res1.submissionId());

        // 3. Submit lần 3 (Khác IKey, nhưng chưa hết 10s -> giả lập 9s trôi qua)
        given(clock.instant()).willReturn(Instant.parse("2026-08-14T10:00:09Z"));
        String ikey2 = UUID.randomUUID().toString();
        assertThatThrownBy(() -> service.execute(attemptId, studentId, quizId, questionId, "code", ikey2, CodeSubmission.Mode.SUBMIT))
                .isInstanceOf(CodeRunnerRateLimitException.class)
                .hasMessageContaining("Rate limit")
                .extracting(ex -> ((CodeRunnerRateLimitException) ex).getRetryAfterSeconds())
                .isEqualTo(1L);

        // 4. Giả lập hết 10s -> Thành công
        given(clock.instant()).willReturn(Instant.parse("2026-08-14T10:00:10Z"));
        CodeRunnerAcceptedView res3 = service.execute(attemptId, studentId, quizId, questionId, "code", ikey2, CodeSubmission.Mode.SUBMIT);
        assertThat(res3.submissionId()).isNotEqualTo(res1.submissionId());
    }

    @Test
    void rateLimit_differentQuestions_notBlocked() throws Exception {
        UUID questionId2 = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.questions (id,content,type,quiz_id,review_status,source_type) VALUES (?,'isEven2','CODE_RUNNER',?,'APPROVED','MANUAL')", questionId2, quizId);
        UUID configId2 = contracts.saveRevision(questionId2, contract("2"));
        contracts.markVerified(questionId2, configId2);
        jdbc.update("INSERT INTO edu.quiz_release_code_runner_bindings (quiz_release_id,question_id,code_runner_config_id) VALUES (?,?,?)", releaseId, questionId2, configId2);

        // Đệ trình đồng thời cho 2 câu hỏi khác nhau trên cùng 1 attempt
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();

        pool.submit(() -> {
            try {
                latch.await();
                service.execute(attemptId, studentId, quizId, questionId, "code", UUID.randomUUID().toString(), CodeSubmission.Mode.SUBMIT);
                success.incrementAndGet();
            } catch (Exception ignored) {}
        });
        pool.submit(() -> {
            try {
                latch.await();
                service.execute(attemptId, studentId, quizId, questionId2, "code", UUID.randomUUID().toString(), CodeSubmission.Mode.SUBMIT);
                success.incrementAndGet();
            } catch (Exception ignored) {}
        });

        latch.countDown();
        pool.shutdown();
        while (!pool.isTerminated()) {
            Thread.sleep(10);
        }

        assertThat(success.get()).isEqualTo(2); // Cả hai phải pass
    }
}
