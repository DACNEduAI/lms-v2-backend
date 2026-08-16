package edu.ai.lms.core.coderunner.adapter.out.persistence;

import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.coderunner.adapter.out.persistence.CodeSubmissionRepository;
import edu.ai.lms.core.coderunner.adapter.out.persistence.CodeSubmissionRow;
import edu.ai.lms.core.coderunner.app.domain.CodeSubmission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import edu.ai.lms.core.coderunner.adapter.out.persistence.CodeRunnerContractPersistenceAdapter;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CrashRecoveryIT extends IntegrationTestConfiguration {

    @Autowired private CodeSubmissionRepository repository;
    @Autowired private CodeRunnerContractPersistenceAdapter contracts;
    @Autowired private JdbcTemplate jdbc;

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

    @Test
    void workerCrashRecovery_jobReacquiredAfterLeaseExpires() {
        UUID studentId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();

        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();

        jdbc.update("INSERT INTO edu.users (id,email,password,role) VALUES (?,?,'x','STUDENT')", studentId, studentId + "@test.local");
        jdbc.update("INSERT INTO edu.students (user_id,student_id,enrollment_year) VALUES (?,?,2026)", studentId, "SV-" + studentId);
        jdbc.update("INSERT INTO edu.users (id,email,password,role) VALUES (?,?,'x','TEACHER')", teacherId, teacherId + "@test.local");
        jdbc.update("INSERT INTO edu.teachers (user_id,employee_id) VALUES (?,?)", teacherId, "GV-" + teacherId);
        jdbc.update("INSERT INTO edu.courses (id,course_name,course_code,active,start_date,end_date) VALUES (?,'DSA',?,true,NOW(),NOW()+INTERVAL '30 days')", courseId, "DSA-" + courseId);
        jdbc.update("INSERT INTO edu.quizzes (id,quiz_title,course_id,teacher_id,lifecycle_status) VALUES (?,'Runner',?,?,'PUBLISHED')", quizId, courseId, teacherId);
        jdbc.update("INSERT INTO edu.questions (id,content,type,quiz_id,review_status,source_type) VALUES (?,'isEven','CODE_RUNNER',?,'APPROVED','MANUAL')", questionId, quizId);
        UUID configId = contracts.saveRevision(questionId, contract("1"));
        UUID releaseId = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.quiz_releases (id,quiz_id,release_number,quiz_version,active,published_by) VALUES (?,?,1,1,true,?)", releaseId, quizId, teacherId);
        jdbc.update("INSERT INTO edu.attempts (id,student_id,quiz_id,quiz_release_id,status,start_time,due_time) VALUES (?,?,?,?,'STARTED',NOW(),NOW()+INTERVAL '30 minutes')", attemptId, studentId, quizId, releaseId);

        jdbc.update("INSERT INTO edu.code_submissions (id, question_id, code_runner_config_id, attempt_id, student_id, source_code, idempotency_key, submission_mode, status, score, max_score) " +
                "VALUES (?, ?, ?, ?, ?, 'code', ?, 'SUBMIT', 'PENDING', 0, 10)",
                submissionId, questionId, configId, attemptId, studentId, UUID.randomUUID().toString());

        // 1. Acquire
        Optional<CodeSubmissionRow> acquired1 = repository.acquireNextPendingSubmission();
        assertThat(acquired1).isPresent();
        assertThat(acquired1.get().getStatus()).isEqualTo(CodeSubmission.Status.PROCESSING.name());
        UUID workerId1 = acquired1.get().getWorkerId();
        assertThat(workerId1).isNotNull();

        // 2. Giả lập crash: Hết lease
        jdbc.update("UPDATE edu.code_submissions SET lease_until = NOW() - INTERVAL '1 MINUTE' WHERE id = ?", submissionId);

        // 3. Acquire lại -> phải nhặt lại được
        Optional<CodeSubmissionRow> acquired2 = repository.acquireNextPendingSubmission();
        assertThat(acquired2).isPresent();
        assertThat(acquired2.get().getId()).isEqualTo(submissionId);
        UUID workerId2 = acquired2.get().getWorkerId();
        assertThat(workerId2).isNotNull();
        assertThat(workerId2).isNotEqualTo(workerId1);

        // 4. Complete
        repository.complete(submissionId, "COMPLETED", "AC", 10.0);
        Optional<CodeSubmissionRow> afterComplete = repository.acquireNextPendingSubmission();
        assertThat(afterComplete).isEmpty();
    }

    @Test
    void concurrentAcquire_noDoubleProcessing() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();

        jdbc.update("INSERT INTO edu.users (id,email,password,role) VALUES (?,?,'x','STUDENT')", studentId, studentId + "@test.local");
        jdbc.update("INSERT INTO edu.students (user_id,student_id,enrollment_year) VALUES (?,?,2026)", studentId, "SV-" + studentId);
        jdbc.update("INSERT INTO edu.users (id,email,password,role) VALUES (?,?,'x','TEACHER')", teacherId, teacherId + "@test.local");
        jdbc.update("INSERT INTO edu.teachers (user_id,employee_id) VALUES (?,?)", teacherId, "GV-" + teacherId);
        jdbc.update("INSERT INTO edu.courses (id,course_name,course_code,active,start_date,end_date) VALUES (?,'DSA',?,true,NOW(),NOW()+INTERVAL '30 days')", courseId, "DSA-" + courseId);
        jdbc.update("INSERT INTO edu.quizzes (id,quiz_title,course_id,teacher_id,lifecycle_status) VALUES (?,'Runner',?,?,'PUBLISHED')", quizId, courseId, teacherId);
        jdbc.update("INSERT INTO edu.questions (id,content,type,quiz_id,review_status,source_type) VALUES (?,'isEven','CODE_RUNNER',?,'APPROVED','MANUAL')", questionId, quizId);
        UUID configId = contracts.saveRevision(questionId, contract("2"));
        UUID releaseId = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.quiz_releases (id,quiz_id,release_number,quiz_version,active,published_by) VALUES (?,?,1,1,true,?)", releaseId, quizId, teacherId);
        jdbc.update("INSERT INTO edu.attempts (id,student_id,quiz_id,quiz_release_id,status,start_time,due_time) VALUES (?,?,?,?,'STARTED',NOW(),NOW()+INTERVAL '30 minutes')", attemptId, studentId, quizId, releaseId);

        List<UUID> submissionIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO edu.code_submissions (id, question_id, code_runner_config_id, attempt_id, student_id, source_code, idempotency_key, submission_mode, status, score, max_score) " +
                    "VALUES (?, ?, ?, ?, ?, 'code', ?, 'SUBMIT', 'PENDING', 0, 10)",
                    id, questionId, configId, attemptId, studentId, UUID.randomUUID().toString());
            submissionIds.add(id);
        }

        Set<UUID> acquiredIds = ConcurrentHashMap.newKeySet();
        AtomicInteger doubleAcquireCount = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            futures.add(pool.submit(() -> {
                Optional<CodeSubmissionRow> job;
                while ((job = repository.acquireNextPendingSubmission()).isPresent()) {
                    UUID id = job.get().getId();
                    if (!acquiredIds.add(id)) {
                        doubleAcquireCount.incrementAndGet();
                    }
                    repository.complete(id, "COMPLETED", "AC", 10.0);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(doubleAcquireCount.get()).isZero();
        assertThat(acquiredIds).hasSize(50);
        Integer completedCount = jdbc.queryForObject("SELECT COUNT(*) FROM edu.code_submissions WHERE status = 'COMPLETED'", Integer.class);
        assertThat(completedCount).isEqualTo(50);
    }
}
