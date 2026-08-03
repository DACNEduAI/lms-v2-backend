package edu.ai.lms.core.attempt.adapter.in.mapper;

import edu.ai.lms.core.attempt.adapter.in.request.SubmitAttemptRequest;
import edu.ai.lms.core.attempt.app.domain.SubmittedAnswer;
import edu.ai.lms.core.attempt.app.port.in.command.SubmitAttemptCommand;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class SubmitAttemptCommandMapper {

    public SubmitAttemptCommand toCommand(UUID courseId, UUID quizId, SubmitAttemptRequest request) {
        List<SubmittedAnswer> submittedAnswers = request.getSubmittedAnswers() == null
                ? Collections.emptyList()
                : request.getSubmittedAnswers()
                .stream()
                .map(item -> SubmittedAnswer.builder()
                        .questionId(item.getQuestionId())
                        .studentAnswerId(item.getStudentAnswerId())
                        .codeSubmissionId(item.getCodeSubmissionId())
                        .build())
                .toList();

        return SubmitAttemptCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .submittedAnswers(submittedAnswers)
                .timeSpentSeconds(request.getTimeSpentSeconds())
                .build();
    }
}
