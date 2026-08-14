package edu.ai.lms.core.quiz.adapter.in.http.request;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizGenerationProgressWebhookRequest {
    private UUID jobId;
    private Integer attempt;
    private Integer generatedQuestionCount;
}
