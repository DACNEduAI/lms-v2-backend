package edu.ai.lms.core.quiz.adapter.in.http.request;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizWebhookRequest {

    private UUID eventId;

    private UUID jobId;

    private String status;

    private QuizPayload quiz;

    private String errorMessage;

    private String errorCode;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuizPayload {

        private String quizTitle;

        @Builder.Default
        private List<QuestionPayload> questions = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionPayload {

        private String content;

        private Question.QuestionType type;

        private String originalType;

        @Builder.Default
        private List<UUID> learningOutcomeIds = new ArrayList<>();

        @Builder.Default
        private List<AnswerPayload> answers = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerPayload {

        private String content;

        private Boolean isCorrect;

        private String explanation;
    }
}
