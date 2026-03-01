package edu.ai.lms.core.quiz.adapter.in.http.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuestionRequest {

    @NotBlank(message = "Question content cannot be blank")
    private String content;

    private String type;

    private Double points;

    @Valid
    @Builder.Default
    private List<CreateAnswerRequest> answers = new ArrayList<>();
}
