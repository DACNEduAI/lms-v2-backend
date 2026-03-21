package edu.ai.lms.core.quiz.adapter.in.http.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

        private Double points;

        @Valid
        @Builder.Default
        private List<CreateAnswerRequest> answers = new ArrayList<>();
    }
}
