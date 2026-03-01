package edu.ai.lms.core.quiz.adapter.in.http.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyQuizResponse {
    private UUID quizId;
    private String quizTitle;
    private Integer timeLimitMinutes;
    private Double passScore;
    private UUID courseId;
    private Boolean active;
    private LocalDateTime updatedAt;
}
