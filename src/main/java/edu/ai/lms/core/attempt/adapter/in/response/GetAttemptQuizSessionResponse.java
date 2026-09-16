package edu.ai.lms.core.attempt.adapter.in.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.ai.lms.config.DatePatternConstants;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Student-safe quiz payload for an already started attempt.  It deliberately
 * does not expose answer correctness or explanations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetAttemptQuizSessionResponse {

    private UUID attemptId;
    private UUID quizId;
    private String quizTitle;
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
    private LocalDateTime dueTime;

    private List<Question> questions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Question {
        private UUID questionId;
        private String content;
        private RichContentFormat contentFormat;
        private String type;
        private List<LearningOutcome> learningOutcomes;
        private List<Answer> answers;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LearningOutcome {
        private UUID id;
        private String code;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Answer {
        private Long answerNumber;
        private String content;
        private RichContentFormat contentFormat;
    }
}
