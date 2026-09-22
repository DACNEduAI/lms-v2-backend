package edu.ai.lms.core.quiz3.adapter.in.http.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteAnswerResponse {
    private Long answerNumber;
    private UUID questionId;
    private String message;
}
