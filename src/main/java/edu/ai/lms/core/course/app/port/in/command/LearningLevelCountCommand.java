package edu.ai.lms.core.course.app.port.in.command;

import java.util.UUID;

import edu.ai.lms.core.course.app.domain.ExamMatrixQuestionType;
import lombok.Builder;

@Builder
public record LearningLevelCountCommand(
        UUID customLearningLevelId,
        String customLearningLevelClientTempId,
        ExamMatrixQuestionType questionType,
        int questionCount) {
}
