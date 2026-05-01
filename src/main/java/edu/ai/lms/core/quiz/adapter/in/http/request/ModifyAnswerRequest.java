package edu.ai.lms.core.quiz.adapter.in.http.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyAnswerRequest {

    private String content;

    private boolean correct;

    private String explanation;
}
