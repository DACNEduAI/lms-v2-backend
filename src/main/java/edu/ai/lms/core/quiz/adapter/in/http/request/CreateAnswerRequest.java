package edu.ai.lms.core.quiz.adapter.in.http.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAnswerRequest {

    @NotBlank(message = "Answer content cannot be blank")
    private String content;

    private boolean isCorrect;

    private String explanation;
}
