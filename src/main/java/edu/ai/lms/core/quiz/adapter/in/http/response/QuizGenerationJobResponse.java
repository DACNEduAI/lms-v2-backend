package edu.ai.lms.core.quiz.adapter.in.http.response;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
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
public class QuizGenerationJobResponse {

    private UUID jobId;

    private QuizGenerationStatus status;

    private LocalDateTime createdAt;
}
