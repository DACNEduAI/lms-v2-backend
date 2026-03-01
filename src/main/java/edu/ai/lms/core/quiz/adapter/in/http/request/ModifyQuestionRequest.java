package edu.ai.lms.core.quiz.adapter.in.http.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyQuestionRequest {

    private String content;

    private String type;

    private Double points;
}
