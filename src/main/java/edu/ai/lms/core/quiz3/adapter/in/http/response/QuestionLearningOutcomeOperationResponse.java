package edu.ai.lms.core.quiz3.adapter.in.http.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionLearningOutcomeOperationResponse {

    private UUID questionId;

    private UUID quizId;

    private UUID courseId;

    private String operation;

    private int totalAffected;

    @Builder.Default
    private List<UUID> learningOutcomeIds = new ArrayList<>();
}
