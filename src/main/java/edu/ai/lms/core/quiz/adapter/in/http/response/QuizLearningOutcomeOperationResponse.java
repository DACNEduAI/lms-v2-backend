package edu.ai.lms.core.quiz.adapter.in.http.response;

import java.util.ArrayList;
import java.util.List;
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
public class QuizLearningOutcomeOperationResponse {

    private UUID quizId;
    private UUID courseId;
    private String operation;
    private int totalAffected;

    @Builder.Default
    private List<UUID> learningOutcomeIds = new ArrayList<>();
}
