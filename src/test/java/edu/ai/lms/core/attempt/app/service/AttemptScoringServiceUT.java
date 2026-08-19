package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.domain.SubmittedAnswer;
import edu.ai.lms.core.attempt.app.exception.InvalidAttemptSubmissionException;
import edu.ai.lms.core.attempt.app.port.out.AttemptCodeRunnerPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptScoringServiceUT {
    @Mock private AttemptCodeRunnerPort codeRunnerPort;
    @InjectMocks private AttemptScoringService service;

    @Test
    void combinesObjectiveAndPartialCodeRunnerScoresWithEqualQuestionWeight() {
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID multipleChoiceId = UUID.randomUUID();
        UUID codeQuestionId = UUID.randomUUID();
        UUID codeSubmissionId = UUID.randomUUID();
        when(codeRunnerPort.isValidSubmit(codeSubmissionId, attemptId, studentId, codeQuestionId)).thenReturn(true);
        when(codeRunnerPort.findCompletedSubmitRatio(
                codeSubmissionId, attemptId, studentId, codeQuestionId)).thenReturn(OptionalDouble.of(0.5));

        AttemptScoringService.ScoringResult result = service.calculateScore(List.of(
                        key(multipleChoiceId, QuestionAnswerKey.QuestionType.MULTIPLE_CHOICE, 2L),
                        key(codeQuestionId, QuestionAnswerKey.QuestionType.CODE_RUNNER, null)),
                List.of(SubmittedAnswer.builder().questionId(multipleChoiceId).studentAnswerId(2L).build(),
                        SubmittedAnswer.builder().questionId(codeQuestionId)
                                .codeSubmissionId(codeSubmissionId).build()), attemptId, studentId);

        assertThat(result.score()).isEqualTo(7.5);
    }



    private QuestionAnswerKey key(UUID id, QuestionAnswerKey.QuestionType type, Long answer) {
        return QuestionAnswerKey.builder().questionId(id).questionType(type).correctAnswerNumber(answer).build();
    }
}
