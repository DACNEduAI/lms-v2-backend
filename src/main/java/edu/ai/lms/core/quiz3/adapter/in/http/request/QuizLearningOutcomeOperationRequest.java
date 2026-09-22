package edu.ai.lms.core.quiz3.adapter.in.http.request;

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
public class QuizLearningOutcomeOperationRequest {

    @Builder.Default
    private List<UUID> learningOutcomeIds = new ArrayList<>();

    @Builder.Default
    private List<UUID> groupLearningOutcomeIds = new ArrayList<>();
}
