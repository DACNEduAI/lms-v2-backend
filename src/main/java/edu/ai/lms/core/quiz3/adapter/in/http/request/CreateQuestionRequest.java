package edu.ai.lms.core.quiz3.adapter.in.http.request;

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
public class CreateQuestionRequest {

    @Valid
    @NotEmpty(message = "questions cannot be empty")
    @Builder.Default
    private List<CreateQuestionItem> questions = new ArrayList<>();



    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateQuestionItem {

        @NotBlank(message = "Question content cannot be blank")
        private String content;

        private String type;

        @NotNull(message = "learningOutcomeId is required")
        private UUID learningOutcomeId;

        @Valid
        @Builder.Default
        private List<CreateAnswerRequest> answers = new ArrayList<>();
    }
}
