package edu.ai.lms.core.quiz.app.service.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationJobResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentRepository;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository;
import edu.ai.lms.core.quiz.adapter.out.persistence.QuestionAiRepository;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AssessmentView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.BaseGenerationView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.LifecycleStatus;
import edu.ai.lms.core.quiz.app.exception.QuizGenerationJobNotFoundException;
import edu.ai.lms.core.quiz.app.port.in.command.GenerateQuizCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.GenerateQuizUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuizGenerationJobUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.QuestionBankUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.service.richcontent.RichContentPolicyService;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceBaseGenerationUT {
    @Mock AssessmentRepository assessments;
    @Mock AssessmentWorkflowRepository workflow;
    @Mock QuestionBankUseCase questionBank;
    @Mock LoadSignedInUserPort signedInUserPort;
    @Mock LoadCourseMembershipPort membershipPort;
    @Mock AssessmentGenerationDispatcher dispatcher;
    @Mock AssessmentDraftValidationService draftValidator;
    @Mock RichContentPolicyService richContentPolicy;
    @Mock QuestionAiRepository questionAi;
    @Mock GenerateQuizUseCase generateQuizUseCase;
    @Mock GetQuizGenerationJobUseCase getQuizGenerationJobUseCase;

    private final UUID courseId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID matrixId = UUID.randomUUID();
    private AssessmentService service;
    private AssessmentView assessment;

    @BeforeEach
    void setUp() {
        service = new AssessmentService(assessments, workflow, questionBank, signedInUserPort,
                membershipPort, dispatcher, draftValidator, new ObjectMapper(), richContentPolicy,
                questionAi, generateQuizUseCase, getQuizGenerationJobUseCase);
        assessment = new AssessmentView(assessmentId, courseId, "Quiz", null, false, 60, 50d,
                null, null, 1, null, null, false, LifecycleStatus.DRAFT, 5,
                UUID.randomUUID(), 1L, matrixId, 1L, false, 1L, actorId, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(signedInUserPort.getAuthenticatedUser())
                .thenReturn(Optional.of(new CurrentActor(actorId, Roles.TEACHER)));
        when(membershipPort.isTeacherInCourse(actorId, courseId)).thenReturn(true);
    }

    private void prepareStart() {
        when(assessments.get(courseId, assessmentId)).thenReturn(Optional.of(assessment));
        when(assessments.unreadyMaterialNames(courseId)).thenReturn(List.of());
        when(assessments.allMaterialIds(courseId)).thenReturn(List.of(UUID.randomUUID()));
        when(assessments.matrixCells(matrixId)).thenReturn(List.of());
    }

    @Test
    void startsGenerationWhenAssessmentHasNoBaseJob() {
        UUID newJobId = UUID.randomUUID();
        prepareStart();
        when(assessments.baseGenerationJobId(courseId, assessmentId)).thenReturn(null);
        when(generateQuizUseCase.generateQuiz(any(GenerateQuizCommand.class)))
                .thenReturn(QuizGenerationJobResponse.builder().jobId(newJobId)
                        .status(QuizGenerationStatus.PENDING).build());

        BaseGenerationView result = service.startBaseGeneration(courseId, assessmentId, 4);

        assertThat(result.jobId()).isEqualTo(newJobId);
        assertThat(result.status()).isEqualTo("PENDING");
        verify(assessments, never()).resetFailedBaseGeneration(any(), any(), any());
    }

    @Test
    void clearsMissingJobReferenceAndStartsReplacementGeneration() {
        UUID staleJobId = UUID.randomUUID();
        UUID replacementJobId = UUID.randomUUID();
        prepareStart();
        when(assessments.baseGenerationJobId(courseId, assessmentId)).thenReturn(staleJobId);
        when(getQuizGenerationJobUseCase.getStatus(staleJobId))
                .thenThrow(new QuizGenerationJobNotFoundException("Quiz generation job not found"));
        when(generateQuizUseCase.generateQuiz(any(GenerateQuizCommand.class)))
                .thenReturn(QuizGenerationJobResponse.builder().jobId(replacementJobId)
                        .status(QuizGenerationStatus.PENDING).build());

        BaseGenerationView result = service.startBaseGeneration(courseId, assessmentId, 4);

        assertThat(result.jobId()).isEqualTo(replacementJobId);
        verify(assessments).resetFailedBaseGeneration(courseId, assessmentId, staleJobId);
        verify(generateQuizUseCase).generateQuiz(any(GenerateQuizCommand.class));
    }

    @Test
    void preservesAnActiveGenerationInsteadOfResettingIt() {
        UUID activeJobId = UUID.randomUUID();
        when(assessments.get(courseId, assessmentId)).thenReturn(Optional.of(assessment));
        QuizGenerationStatusResponse active = QuizGenerationStatusResponse.builder()
                .jobId(activeJobId).status(QuizGenerationStatus.PENDING).attemptCount(1).build();
        when(assessments.baseGenerationJobId(courseId, assessmentId)).thenReturn(activeJobId);
        when(getQuizGenerationJobUseCase.getStatus(activeJobId)).thenReturn(active);

        BaseGenerationView result = service.startBaseGeneration(courseId, assessmentId, 4);

        assertThat(result.jobId()).isEqualTo(activeJobId);
        assertThat(result.status()).isEqualTo("PENDING");
        verify(assessments, never()).resetFailedBaseGeneration(any(), any(), any());
        verify(generateQuizUseCase, never()).generateQuiz(any());
    }

    @Test
    void resetsFailedGenerationBeforeStartingAnother() {
        UUID failedJobId = UUID.randomUUID();
        UUID replacementJobId = UUID.randomUUID();
        prepareStart();
        when(assessments.baseGenerationJobId(courseId, assessmentId)).thenReturn(failedJobId);
        when(getQuizGenerationJobUseCase.getStatus(failedJobId)).thenReturn(
                QuizGenerationStatusResponse.builder().jobId(failedJobId)
                        .status(QuizGenerationStatus.FAILED).build());
        when(generateQuizUseCase.generateQuiz(any(GenerateQuizCommand.class)))
                .thenReturn(QuizGenerationJobResponse.builder().jobId(replacementJobId)
                        .status(QuizGenerationStatus.PENDING).build());

        BaseGenerationView result = service.startBaseGeneration(courseId, assessmentId, 4);

        assertThat(result.jobId()).isEqualTo(replacementJobId);
        verify(assessments).resetFailedBaseGeneration(courseId, assessmentId, failedJobId);
    }

    @Test
    void clearsMissingJobReferenceWhenRefreshingGenerationStatus() {
        UUID staleJobId = UUID.randomUUID();
        when(assessments.baseGenerationJobId(courseId, assessmentId)).thenReturn(staleJobId);
        when(getQuizGenerationJobUseCase.getStatus(staleJobId))
                .thenThrow(new QuizGenerationJobNotFoundException("Quiz generation job not found"));

        BaseGenerationView result = service.reconcileBaseGeneration(courseId, assessmentId);

        assertThat(result.status()).isEqualTo("NOT_STARTED");
        assertThat(result.jobId()).isNull();
        verify(assessments).resetFailedBaseGeneration(courseId, assessmentId, staleJobId);
    }
}
