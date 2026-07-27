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
public class ExamMatrixRow {
    private UUID id;
    private UUID matrixId;
    private UUID chapterId;
    private String topic;
    private int orderIndex;

    @Builder.Default
    private List<UUID> learningOutcomeIds = new ArrayList<>();

    @Builder.Default
    private List<ExamMatrixLevelCount> levelCounts = new ArrayList<>();
}
