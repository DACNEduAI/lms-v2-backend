package edu.ai.lms.core.attempt.app.port.out;

import edu.ai.lms.core.attempt.app.domain.QuestionGradeResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Stores the final MCQ response snapshot for an attempt. */
public interface AttemptQuestionResponsePort {

    void replaceFinalMultipleChoiceResponses(
            UUID attemptId,
            List<QuestionGradeResult> questionResults,
            LocalDateTime finalizedAt
    );
}
