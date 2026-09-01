package edu.ai.lms.core.coderunner.app.service;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.auth.api.CurrentPrincipalApi;
import edu.ai.lms.core.auth.api.AuthenticatedPrincipal;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerNotFoundException;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CodeRunnerContractServiceIT extends IntegrationTestConfiguration {

    @Autowired
    private CodeRunnerContractService service;

    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
    private CurrentPrincipalApi currentPrincipalApi;

    @MockBean
    private edu.ai.lms.core.auth.api.TokenAuthenticationApi tokenAuthenticationApi;

    @MockBean
    private CodeExecutionEngine executionEngine;

    @MockBean
    private edu.ai.lms.core.coderunner.app.port.out.CheckCodeRunnerCourseAccessPort courseAccessPort;

    @MockBean
    private edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerQuestionContextPort questionContextPort;

    private UUID teacherId = UUID.randomUUID();
    private UUID courseId = UUID.randomUUID();
    private UUID quizId = UUID.randomUUID();
    private UUID questionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Never delete shared Testcontainers fixtures from another IT class.
        // Each test owns a fresh graph instead.
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        
        jdbc.update("INSERT INTO courses (id, course_name, course_code, start_date, end_date) VALUES (?, ?, ?, NOW(), NOW())", courseId, "Test Course", "TEST-" + courseId);
        jdbc.update("INSERT INTO users (id, email, password, role) VALUES (?, ?, 'pwd', 'TEACHER')", teacherId, teacherId + "@test.com");
        jdbc.update("INSERT INTO teachers (user_id, employee_id, department) VALUES (?, ?, 'Test Dept')", teacherId, "EMP-" + teacherId);
        
        jdbc.update("INSERT INTO quizzes (id, course_id, teacher_id, quiz_title, lifecycle_status, version, created_at, updated_at) VALUES (?, ?, ?, 'Quiz', 'DRAFT', 1, NOW(), NOW())",
                quizId, courseId, teacherId);
        
        jdbc.update("INSERT INTO questions (id, quiz_id, type, content) VALUES (?, ?, 'CODE_RUNNER', 'Question content')",
                questionId, quizId);
        
        org.mockito.Mockito.when(courseAccessPort.isTeacherInCourse(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        org.mockito.Mockito.when(questionContextPort.findQuestionContext(questionId)).thenReturn(java.util.Optional.of(
                new edu.ai.lms.core.coderunner.app.domain.CodeRunnerQuestionContext(questionId, quizId, courseId, teacherId, "CODE_RUNNER", "NOT_VALIDATED", "DRAFT", 1L, null)
        ));
        
        when(currentPrincipalApi.currentPrincipal())
                .thenReturn(Optional.of(new AuthenticatedPrincipal(teacherId, "teacher@test.com", Roles.TEACHER, java.util.Set.of())));
    }

    @Test
    void testContractLifecycle_Save_Verify_Edit_Reverify_Publish() {
        // 1. Initial GET throws NotFound
        assertThatThrownBy(() -> service.getStoredForTeacher(courseId, quizId, questionId))
                .isInstanceOf(CodeRunnerNotFoundException.class);

        // 2. Save contract (creates READY_FOR_REVIEW)
        ExecutableAssessmentContract contract = createDummyContract();
        ExecutableAssessmentContract saved = service.save(courseId, quizId, questionId, contract);
        assertThat(saved.status()).isEqualTo("READY_FOR_REVIEW");

        // 3. GET after Save
        ExecutableAssessmentContract retrieved = service.getForTeacher(courseId, quizId, questionId);
        assertThat(retrieved.status()).isEqualTo("READY_FOR_REVIEW");

        // 4. Verify
        when(executionEngine.submit(any())).thenReturn(
                new CodeExecutionEngine.ExecutionTicket("sub-1"));
        when(executionEngine.getResult("sub-1")).thenReturn(
                new CodeExecutionEngine.ExecutionResult(CodeExecutionEngine.EngineState.FINISHED, CodeExecutionEngine.Verdict.ACCEPTED, "I:3", "", "", 10.0, 1024L, ""));

        var verifyResponse = service.verify(courseId, quizId, questionId);
        assertThat(verifyResponse.passed()).isTrue();

        // Check DB status is VERIFIED_EXECUTION_GOLD
        ExecutableAssessmentContract verified = service.getForTeacher(courseId, quizId, questionId);
        assertThat(verified.status()).isEqualTo("VERIFIED_EXECUTION_GOLD");

        // 5. Edit contract (creates new revision READY_FOR_REVIEW)
        ExecutableAssessmentContract edited = new ExecutableAssessmentContract(
                "2.0.0", "con_1", "1.0.0", "DRAFT", "Title updated", "Stmt updated", contract.runtime(), contract.privateGrading());
        service.save(courseId, quizId, questionId, edited);

        ExecutableAssessmentContract getAfterEdit = service.getForTeacher(courseId, quizId, questionId);
        assertThat(getAfterEdit.status()).isEqualTo("READY_FOR_REVIEW");
        assertThat(getAfterEdit.title()).isEqualTo("Title updated");

        // 6. Reverify new revision
        when(executionEngine.submit(any())).thenReturn(
                new CodeExecutionEngine.ExecutionTicket("sub-2"));
        when(executionEngine.getResult("sub-2")).thenReturn(
                new CodeExecutionEngine.ExecutionResult(CodeExecutionEngine.EngineState.FINISHED, CodeExecutionEngine.Verdict.ACCEPTED, "I:3", "", "", 10.0, 1024L, ""));

        var reverifyResponse = service.verify(courseId, quizId, questionId);
        assertThat(reverifyResponse.passed()).isTrue();
        
        ExecutableAssessmentContract reverified = service.getForTeacher(courseId, quizId, questionId);
        assertThat(reverified.status()).isEqualTo("VERIFIED_EXECUTION_GOLD");
    }

    @Test
    void testDraftContract_Verify() throws Exception {
        // Draft JSON passed through context
        ExecutableAssessmentContract contract = createDummyContract();
        String draftJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(contract);
        
        // Ensure no contract in DB yet
        assertThatThrownBy(() -> service.getStoredForTeacher(courseId, quizId, questionId))
                .isInstanceOf(CodeRunnerNotFoundException.class);
                
        org.mockito.Mockito.when(questionContextPort.findQuestionContext(questionId)).thenReturn(java.util.Optional.of(
                new edu.ai.lms.core.coderunner.app.domain.CodeRunnerQuestionContext(questionId, quizId, courseId, teacherId, "CODE_RUNNER", "NOT_VALIDATED", "DRAFT", 1L, draftJson)
        ));

        when(executionEngine.submit(any())).thenReturn(
                new CodeExecutionEngine.ExecutionTicket("sub-3"));
        when(executionEngine.getResult("sub-3")).thenReturn(
                new CodeExecutionEngine.ExecutionResult(CodeExecutionEngine.EngineState.FINISHED, CodeExecutionEngine.Verdict.ACCEPTED, "I:3", "", "", 10.0, 1024L, ""));

        // Verify should read the draft JSON from questionContextPort (which reads from DB), save it to a real revision, and then verify it
        var verifyResponse = service.verify(courseId, quizId, questionId);
        assertThat(verifyResponse.passed()).isTrue();
        
        // Now it should be saved and verified in DB
        ExecutableAssessmentContract verified = service.getForTeacher(courseId, quizId, questionId);
        assertThat(verified.status()).isEqualTo("VERIFIED_EXECUTION_GOLD");
    }

    private ExecutableAssessmentContract createDummyContract() {
        var integer = new ExecutableAssessmentContract.VersionedRef("integer", "1.0.0");
        var signature = new ExecutableAssessmentContract.FunctionSignature(integer, List.of());
        var runtime = new ExecutableAssessmentContract.RuntimeContract(
                new ExecutableAssessmentContract.VersionedRef("dsa_cpp17", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("scalar_function", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("exact_typed", "1.0.0"),
                "sum", signature, "int sum() { return 3; }",
                List.of(
                        new ExecutableAssessmentContract.TestCase("t1", "cat", "PUBLIC", new java.util.HashMap<>(), new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(3), 0)
                ),
                List.of()
        );
        return new ExecutableAssessmentContract(
                "2.0.0", "con_1", "1.0.0", "DRAFT", "Title", "Stmt", runtime,
                new ExecutableAssessmentContract.PrivateGrading("int sum() { return 3; }", List.of(
                        new ExecutableAssessmentContract.TestCase("t2", "cat", "HIDDEN", new java.util.HashMap<>(), new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(3), 10)
                ))
        );
    }
}
