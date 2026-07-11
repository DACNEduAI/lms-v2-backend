package edu.ai.lms.core.quiz.adapter.in.http.response;

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
public class GetQuestionLearningOutcomesResponse {

    private UUID questionId;

    private UUID quizId;

    private UUID courseId;

    @Builder.Default
    private List<LearningOutcomeSummary> learningOutcomes = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LearningOutcomeSummary {

        private UUID id;

        private String code;
        private Integer prefixNumber;

        private String description;
    }
}
