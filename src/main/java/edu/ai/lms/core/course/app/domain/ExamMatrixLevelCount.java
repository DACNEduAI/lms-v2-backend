package edu.ai.lms.core.course.app.domain;

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
public class ExamMatrixLevelCount {
    private UUID customLearningLevelId;
    @Builder.Default
    private List<UUID> learningOutcomeIds = new ArrayList<>();
    private String code;
    private String name;
    private String description;
    private int sortOrder;
    private int questionCount;
}
