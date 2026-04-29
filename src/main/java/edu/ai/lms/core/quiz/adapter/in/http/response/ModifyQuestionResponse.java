package edu.ai.lms.core.quiz.adapter.in.http.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import edu.ai.lms.config.DatePatternConstants;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyQuestionResponse {
    private UUID questionId;
    private String content;
    private String type;
    private UUID quizId;
    private List<AnswerSummary> answers;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerSummary {
        private Long answerNumber;
        private String content;
        private boolean isCorrect;
        private String explanation;
    }
}
