package edu.ai.lms.core.quiz.adapter.in.http.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuizRequest {

    @NotBlank(message = "Quiz title cannot be blank")
    private String quizTitle;

    private Integer timeLimitMinutes;

    private Double passScore;
}
