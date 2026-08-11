package edu.ai.lms.core.quiz.app.service.assessment;

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentRepository;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AssessmentView;
import edu.ai.lms.core.quiz.app.port.in.usecase.QuestionBankUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceContextUT {
    @Mock AssessmentRepository assessments;
    @Mock AssessmentWorkflowRepository workflow;
    @Mock QuestionBankUseCase questionBank;
    @Mock LoadSignedInUserPort signedInUserPort;
    @Mock LoadCourseMembershipPort membershipPort;
    @Mock AssessmentGenerationDispatcher dispatcher;
    @Mock ObjectMapper objectMapper;
    @InjectMocks AssessmentService service;

    private final UUID courseId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void teacherAccess() {
        when(signedInUserPort.getAuthenticatedUser())
                .thenReturn(Optional.of(new CurrentActor(actorId, Roles.TEACHER)));
        when(membershipPort.isTeacherInCourse(actorId, courseId)).thenReturn(true);
        when(assessments.get(courseId, assessmentId)).thenReturn(Optional.of(mock(AssessmentView.class)));
        when(assessments.listCcctProfiles(courseId)).thenReturn(List.of());
        when(assessments.listExamMatrices(courseId)).thenReturn(List.of());
    }

    @Test
    void resolvesCopyModesOnServer() {
        UUID ccctSource = UUID.randomUUID();
        UUID matrixSource = UUID.randomUUID();
        UUID ccctCopy = UUID.randomUUID();
        UUID matrixCopy = UUID.randomUUID();
        when(assessments.cloneCcct(courseId, ccctSource, actorId)).thenReturn(ccctCopy);
        when(assessments.cloneMatrix(courseId, matrixSource)).thenReturn(matrixCopy);

        service.selectContext(courseId, assessmentId, new AssessmentService.ContextSelection(
                new AssessmentService.SourceSelection(
                        AssessmentService.SourceSelectionMode.COPY_AS_DRAFT, ccctSource, null),
                new AssessmentService.SourceSelection(
                        AssessmentService.SourceSelectionMode.COPY_AS_DRAFT, matrixSource, null)));

        verify(assessments).selectSources(courseId, assessmentId, ccctCopy, matrixCopy);
    }

    @Test
    void createsNewDraftsWithTrimmedNames() {
        UUID ccctDraft = UUID.randomUUID();
        UUID matrixDraft = UUID.randomUUID();
        when(assessments.createCcctDraft(courseId, "CCCT 2026", actorId)).thenReturn(ccctDraft);
        when(assessments.createMatrixDraft(courseId, "Matrix 2026")).thenReturn(matrixDraft);

        service.selectContext(courseId, assessmentId, new AssessmentService.ContextSelection(
                new AssessmentService.SourceSelection(
                        AssessmentService.SourceSelectionMode.CREATE_NEW_DRAFT, null, "  CCCT 2026  "),
                new AssessmentService.SourceSelection(
                        AssessmentService.SourceSelectionMode.CREATE_NEW_DRAFT, null, " Matrix 2026 ")));

        verify(assessments).selectSources(courseId, assessmentId, ccctDraft, matrixDraft);
    }
}
