package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.port.out.AttemptCodeRunnerPort;
import edu.ai.lms.core.attempt.app.port.out.AttemptCodeSubmissionSnapshotPort;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.SaveAttemptPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptGradingServiceUT {

    @Mock private LoadAttemptPort attempts;
    @Mock private SaveAttemptPort saveAttempts;
    @Mock private LoadQuizAttemptPolicyPort policy;
    @Mock private AttemptCodeRunnerPort codeRunner;
    @Mock private AttemptCodeSubmissionSnapshotPort snapshots;
    @InjectMocks private AttemptGradingService service;

    @Test
    void recoveryFinalizesCompletedSelectedSubmissionAfterEventLoss() {
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        Attempt attempt = Attempt.builder()
                .id(attemptId).studentId(studentId).quizId(quizId)
                .status(Attempt.AttemptStatus.FINALIZING).score(0.0).build();

        when(attempts.getFinalizingAttempts()).thenReturn(List.of(attempt));
        when(snapshots.findByAttemptId(attemptId)).thenReturn(Map.of(questionId, submissionId));
        when(codeRunner.isTerminalSubmit(submissionId, attemptId, studentId, questionId)).thenReturn(true);
        when(policy.getQuestionAnswerKeys(quizId)).thenReturn(List.of(
                QuestionAnswerKey.builder().questionId(questionId)
                        .questionType(QuestionAnswerKey.QuestionType.CODE_RUNNER).build()));
        when(codeRunner.findCompletedSubmitRatio(submissionId, attemptId, studentId, questionId))
                .thenReturn(OptionalDouble.of(1.0));

        service.recoverFinalizingAttempts();

        verify(saveAttempts).finalizeIfStillFinalizing(eq(attemptId), eq(10.0));
    }

    @Test
    void recoveryLeavesAttemptFinalizingWhileSelectedSubmissionIsPending() {
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        Attempt attempt = Attempt.builder().id(attemptId).studentId(studentId)
                .status(Attempt.AttemptStatus.FINALIZING).build();

        when(attempts.getFinalizingAttempts()).thenReturn(List.of(attempt));
        when(snapshots.findByAttemptId(attemptId)).thenReturn(Map.of(questionId, submissionId));
        when(codeRunner.isTerminalSubmit(submissionId, attemptId, studentId, questionId)).thenReturn(false);

        service.recoverFinalizingAttempts();

        org.mockito.Mockito.verifyNoInteractions(saveAttempts);
    }
}
