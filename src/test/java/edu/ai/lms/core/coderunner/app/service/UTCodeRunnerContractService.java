package edu.ai.lms.core.coderunner.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.coderunner.app.domain.CodeRunnerActor;
import edu.ai.lms.core.coderunner.app.domain.CodeRunnerQuestionContext;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerConflictException;
import edu.ai.lms.core.coderunner.adapter.in.http.response.CodeRunnerVerificationResponse;
import edu.ai.lms.core.coderunner.app.port.out.CheckCodeRunnerCourseAccessPort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerActorPort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerContractPort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerQuestionContextPort;
import edu.ai.lms.core.coderunner.app.port.out.SaveCodeRunnerContractPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UTCodeRunnerContractService {

    @Mock private LoadCodeRunnerActorPort actorPort;
    @Mock private LoadCodeRunnerQuestionContextPort questionContextPort;
    @Mock private CheckCodeRunnerCourseAccessPort courseAccessPort;
    @Mock private SaveCodeRunnerContractPort saveContractPort;
    @Mock private LoadCodeRunnerContractPort loadContractPort;
    @Mock private CodeRunnerVerificationService verificationService;

    private CodeRunnerContractService service;
    private UUID teacherId;
    private UUID courseId;
    private UUID quizId;
    private UUID questionId;

    @BeforeEach
    void setUp() {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        service = new CodeRunnerContractService(
                actorPort, questionContextPort, courseAccessPort, saveContractPort, loadContractPort, verificationService, objectMapper);
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        when(actorPort.loadCurrentActor()).thenReturn(Optional.of(new CodeRunnerActor(teacherId, Roles.TEACHER)));
        when(questionContextPort.findQuestionContext(questionId)).thenReturn(Optional.of(
                new CodeRunnerQuestionContext(questionId, quizId, courseId, teacherId,
                        "CODE_RUNNER", "APPROVED", "DRAFT", 1L, null)));
        when(courseAccessPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);
    }

    @Test
    void alwaysSavesAsReadyForReviewEvenWhenClientClaimsVerifiedStatus() {
        ExecutableAssessmentContract submitted = contract("VERIFIED_EXECUTION_GOLD");
        ExecutableAssessmentContract stored = submitted.withStatus("READY_FOR_REVIEW");
        when(saveContractPort.saveRevision(any(), any())).thenReturn(UUID.randomUUID());
        when(loadContractPort.findLatestByQuestionId(questionId)).thenReturn(Optional.of(stored));

        ExecutableAssessmentContract response = service.save(courseId, quizId, questionId, submitted);

        ArgumentCaptor<ExecutableAssessmentContract> saved =
                ArgumentCaptor.forClass(ExecutableAssessmentContract.class);
        verify(saveContractPort).saveRevision(org.mockito.ArgumentMatchers.eq(questionId), saved.capture());
        assertThat(saved.getValue().status()).isEqualTo("READY_FOR_REVIEW");
        assertThat(response.status()).isEqualTo("READY_FOR_REVIEW");
    }

    @Test
    void teacherCanTryAnUnsavedCandidateWithoutCreatingAContractRevision() {
        ExecutableAssessmentContract submitted = contract("VERIFIED_EXECUTION_GOLD");
        CodeRunnerVerificationResponse expected = new CodeRunnerVerificationResponse(
                true, 2, 2, List.of());
        when(verificationService.tryCandidate(any(), any(), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(expected);

        CodeRunnerVerificationResponse response = service.tryCandidate(
                courseId, quizId, questionId, submitted,
                "bool isEven(int value) { return value % 2 == 0; }", true);

        ArgumentCaptor<ExecutableAssessmentContract> contract =
                ArgumentCaptor.forClass(ExecutableAssessmentContract.class);
        verify(verificationService).tryCandidate(
                contract.capture(),
                org.mockito.ArgumentMatchers.eq("bool isEven(int value) { return value % 2 == 0; }"),
                org.mockito.ArgumentMatchers.eq(true));
        verify(saveContractPort, never()).saveRevision(any(), any());
        assertThat(contract.getValue().status()).isEqualTo("READY_FOR_REVIEW");
        assertThat(response).isSameAs(expected);
    }



    private ExecutableAssessmentContract contract(String status) {
        ObjectMapper mapper = new ObjectMapper();
        var integer = new ExecutableAssessmentContract.VersionedRef("integer", "1.0.0");
        var bool = new ExecutableAssessmentContract.VersionedRef("boolean", "1.0.0");
        var signature = new ExecutableAssessmentContract.FunctionSignature(bool,
                List.of(new ExecutableAssessmentContract.ArgumentSpec("value", integer, Map.of())));
        Map<String, com.fasterxml.jackson.databind.JsonNode> publicArgs = new LinkedHashMap<>();
        publicArgs.put("value", mapper.valueToTree(2));
        Map<String, com.fasterxml.jackson.databind.JsonNode> hiddenArgs = new LinkedHashMap<>();
        hiddenArgs.put("value", mapper.valueToTree(-3));
        var publicTest = new ExecutableAssessmentContract.TestCase(
                "public_001", "example", "PUBLIC", publicArgs, mapper.valueToTree(true), 0);
        var hiddenTest = new ExecutableAssessmentContract.TestCase(
                "hidden_001", "negative", "HIDDEN", hiddenArgs, mapper.valueToTree(false), 1);
        var runtime = new ExecutableAssessmentContract.RuntimeContract(
                new ExecutableAssessmentContract.VersionedRef("dsa_cpp17", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("scalar_function", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("exact_typed", "1.0.0"),
                "isEven", signature, "bool isEven(int value) { return false; }",
                List.of(publicTest), List.of("\\bsystem\\s*\\("));
        return new ExecutableAssessmentContract(
                "2.0.0", "is_even_v1", "1.0.0", status, "Even number",
                "Return true when the integer is even.", runtime,
                new ExecutableAssessmentContract.PrivateGrading(
                        "bool isEven(int value) { return value % 2 == 0; }", List.of(hiddenTest)));
    }
}
