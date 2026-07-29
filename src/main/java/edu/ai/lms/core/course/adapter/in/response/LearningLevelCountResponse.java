package edu.ai.lms.core.course.adapter.in.response;

import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.ExamMatrixQuestionType;
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
public class LearningLevelCountResponse {
    private UUID customLearningLevelId;
    private List<UUID> learningOutcomeIds;
    private String code;
    private String name;
    private String description;
    private int sortOrder;
    private ExamMatrixQuestionType questionType;
    private int questionCount;
    private boolean active;
}
