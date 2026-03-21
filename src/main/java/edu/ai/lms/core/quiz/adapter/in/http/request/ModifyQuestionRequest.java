package edu.ai.lms.core.quiz.adapter.in.http.request;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyQuestionRequest {

    @Valid
    @NotEmpty(message = "questions cannot be empty")
    @Builder.Default
    private List<ModifyQuestionItem> questions = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModifyQuestionItem {

        @NotNull(message = "questionId is required")
        private UUID questionId;

        @NotBlank(message = "content is required")
        private String content;

        private String type;

        private Double points;

        @Valid
        @Builder.Default
        private List<CreateAnswerRequest> answers = new ArrayList<>();
    }
}
