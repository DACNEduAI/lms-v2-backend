package edu.ai.lms.core.quiz.app.service.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentRepository;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AssessmentView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.BatchValidationResult;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.DraftReviewPage;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.DraftReviewSearch;
import edu.ai.lms.core.quiz.app.port.in.usecase.QuestionBankUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.service.richcontent.RichContentPolicyService;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceReviewUT {
    @Mock AssessmentRepository assessments;
    @Mock AssessmentWorkflowRepository workflow;
    @Mock QuestionBankUseCase questionBank;
    @Mock LoadSignedInUserPort signedInUserPort;
    @Mock LoadCourseMembershipPort membershipPort;
    @Mock AssessmentGenerationDispatcher dispatcher;
    @Mock AssessmentDraftValidationService draftValidator;
    @Mock RichContentPolicyService richContentPolicy;

    private AssessmentService service;
    private final UUID courseId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private AssessmentView assessment;

    @BeforeEach
    void setUp() {
        service = new AssessmentService(assessments, workflow, questionBank, signedInUserPort,
                membershipPort, dispatcher, draftValidator, new ObjectMapper(), richContentPolicy);
        assessment = mock(AssessmentView.class);
        when(signedInUserPort.getAuthenticatedUser())
                .thenReturn(Optional.of(new CurrentActor(actorId, Roles.TEACHER)));
        when(membershipPort.isTeacherInCourse(actorId, courseId)).thenReturn(true);
        when(assessments.get(courseId, assessmentId)).thenReturn(Optional.of(assessment));
    }

    @Test
    void normalizesPagedReviewRequest() {
        DraftReviewPage expected = new DraftReviewPage(assessmentId, List.of(), 0, 0, 25);
        when(workflow.reviewPage(eq(assessmentId), any())).thenReturn(expected);

        service.reviewPage(courseId, assessmentId, new DraftReviewSearch(" question ", "NOT_VALIDATED",
                null, null, null, null, null, -3, 17));

        ArgumentCaptor<DraftReviewSearch> captor = ArgumentCaptor.forClass(DraftReviewSearch.class);
        verify(workflow).reviewPage(eq(assessmentId), captor.capture());
        assertEquals("question", captor.getValue().query());
        assertEquals(0, captor.getValue().page());
        assertEquals(25, captor.getValue().size());
    }

    @Test
    void batchValidationKeepsSuccessfulDraftsWhenOneConflicts() {
        UUID first = UUID.randomUUID();
        UUID stale = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        when(assessment.optionCount()).thenReturn(4);
        doAnswer(invocation -> {
            if (stale.equals(invocation.getArgument(2))) {
                throw new IllegalStateException("Draft changed elsewhere");
            }
            return null;
        }).when(draftValidator).validate(eq(courseId), eq(assessmentId), any(UUID.class), anyLong(), eq(4));

        BatchValidationResult result = service.validateBatch(courseId, assessmentId, List.of(
                new AssessmentService.DraftVersion(first, 1),
                new AssessmentService.DraftVersion(stale, 2),
                new AssessmentService.DraftVersion(third, 3)));

        assertEquals(List.of("VALIDATED", "FAILED", "VALIDATED"),
                result.results().stream().map(item -> item.status()).toList(), result.results().toString());
        assertEquals(2, result.validatedCount());
        assertEquals(1, result.failedCount());
        assertEquals("FAILED", result.results().get(1).status());
        verify(draftValidator).validate(courseId, assessmentId, first, 1, 4);
        verify(draftValidator).validate(courseId, assessmentId, third, 3, 4);
        verify(workflow, never()).review(assessmentId);
    }
}
