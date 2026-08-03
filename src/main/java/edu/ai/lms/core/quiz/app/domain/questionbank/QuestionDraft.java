package edu.ai.lms.core.quiz.app.domain.questionbank;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;
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
public class QuestionDraft {
    private UUID id;
    private UUID courseId;
    private UUID chapterId;
    private UUID cognitiveLevelId;
    private CognitiveLevelCode cognitiveLevelCode;
    private String cognitiveLevelName;
    private String content;
    private Question.QuestionType questionType;
    @Builder.Default
    private QuestionBankSourceType sourceType = QuestionBankSourceType.MANUAL;
    private UUID createdBy;
    @Builder.Default
    private long version = 0;
    @Builder.Default
    private List<QuestionBankAnswer> answers = new ArrayList<>();
    @Builder.Default
    private Set<UUID> learningOutcomeIds = new LinkedHashSet<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
