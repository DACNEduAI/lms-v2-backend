package edu.ai.lms.core.quiz.adapter.in.http.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyQuizRequest {

    private String quizTitle;

    private Integer timeLimitMinutes;

    private Double passScore;

    private Boolean active;
}
