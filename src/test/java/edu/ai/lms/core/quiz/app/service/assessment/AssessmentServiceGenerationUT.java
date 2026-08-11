package edu.ai.lms.core.quiz.app.service.assessment;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

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
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentRepository;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository.DraftProvenance;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AssessmentView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.BankPlan;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.BankPlanCell;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.BatchView;
import edu.ai.lms.core.quiz.app.port.in.usecase.QuestionBankUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceGenerationUT {
    @Mock AssessmentRepository assessments;
    @Mock AssessmentWorkflowRepository workflow;
    @Mock QuestionBankUseCase questionBank;
    @Mock LoadSignedInUserPort signedInUserPort;
    @Mock LoadCourseMembershipPort membershipPort;
    @Mock AssessmentGenerationDispatcher dispatcher;
    @Mock AssessmentDraftValidationService draftValidator;

    private AssessmentService service;
    private final UUID courseId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private AssessmentView assessment;

    @BeforeEach
    void setUp() {
        service = new AssessmentService(assessments, workflow, questionBank, signedInUserPort,
                membershipPort, dispatcher, draftValidator, new ObjectMapper());
        assessment = mock(AssessmentView.class);
        when(signedInUserPort.getAuthenticatedUser())
                .thenReturn(Optional.of(new CurrentActor(actorId, Roles.TEACHER)));
        when(membershipPort.isTeacherInCourse(actorId, courseId)).thenReturn(true);
        when(assessments.get(courseId, assessmentId)).thenReturn(Optional.of(assessment));
    }

    @Test
    void generateReturnsExistingActiveBatchInsteadOfCreatingAnother() {
        BatchView active = mock(BatchView.class);
        when(workflow.activeBatch(courseId, assessmentId)).thenReturn(active);

        BatchView result = service.generateMissing(courseId, assessmentId);

        assertSame(active, result);
        verify(workflow, never()).createBatch(any(), any(), any(), any());
        verify(assessments, never()).allMaterialIds(courseId);
    }

    @Test
    void regenerateDeletesDraftAndDispatchesOneCellBatch() {
        UUID draftId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        UUID matrixId = UUID.randomUUID();
        when(assessment.id()).thenReturn(assessmentId);
        when(assessment.courseId()).thenReturn(courseId);
        when(assessment.examMatrixId()).thenReturn(matrixId);
        when(assessment.optionCount()).thenReturn(4);
        BankPlanCell source = new BankPlanCell(rowId, levelId, UUID.randomUUID(), "Chapter 1", "Topic",
                "UNDERSTAND", "Understand", List.of(UUID.randomUUID()), 3, 0, 0, 0, 0, 3);
        BankPlan plan = new BankPlan(assessmentId, matrixId, false,
                3, 0, 0, 0, 0, 3, List.of(source));
        BatchView created = mock(BatchView.class);
        UUID batchId = UUID.randomUUID();
        when(created.id()).thenReturn(batchId);
        when(workflow.draftProvenance(assessmentId, draftId))
                .thenReturn(new DraftProvenance(UUID.randomUUID(), rowId, levelId));
        when(workflow.bankPlan(assessment)).thenReturn(plan);
        when(assessments.unreadyMaterialNames(courseId)).thenReturn(List.of());
        when(assessments.allMaterialIds(courseId)).thenReturn(List.of(materialId));
        when(assessments.courseTitle(courseId)).thenReturn("Course");
        when(workflow.createBatch(eq(assessment), any(BankPlan.class), eq(actorId), anyString()))
                .thenReturn(created);

        BatchView result = service.regenerateDraft(courseId, assessmentId, draftId, 4);

        assertSame(created, result);
        verify(questionBank).deleteDraft(courseId, draftId, 4);
        verify(dispatcher).dispatch(batchId);
    }
}
