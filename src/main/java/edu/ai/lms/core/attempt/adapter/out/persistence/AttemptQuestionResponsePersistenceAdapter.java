package edu.ai.lms.core.attempt.adapter.out.persistence;

import edu.ai.lms.core.attempt.app.domain.QuestionGradeResult;
import edu.ai.lms.core.attempt.app.port.out.AttemptQuestionResponsePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AttemptQuestionResponsePersistenceAdapter implements AttemptQuestionResponsePort {

    private final AttemptQuestionResponseRepository repository;

    @Override
    public void replaceFinalMultipleChoiceResponses(
            UUID attemptId,
            List<QuestionGradeResult> questionResults,
            LocalDateTime finalizedAt
    ) {
        repository.deleteByAttemptId(attemptId);
        questionResults.stream()
                .filter(QuestionGradeResult::isMultipleChoice)
                .forEach(result -> repository.insert(attemptId, result, finalizedAt));
    }
}
