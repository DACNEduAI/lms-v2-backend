package edu.ai.lms.core.course.adapter.in.request;

import java.util.UUID;

import edu.ai.lms.core.course.app.domain.ExamMatrixQuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class LearningLevelCountRequest {
    private UUID customLearningLevelId;
    private String customLearningLevelClientTempId;

    @NotNull(message = "questionType cannot be null")
    private ExamMatrixQuestionType questionType;

    @Min(value = 0, message = "questionCount must be >= 0")
    private int questionCount;
}
