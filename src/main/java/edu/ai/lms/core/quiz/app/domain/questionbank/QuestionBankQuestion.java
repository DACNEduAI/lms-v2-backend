package edu.ai.lms.core.quiz.app.domain.questionbank;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
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
public class QuestionBankQuestion {
    private UUID id;
    private UUID originDraftId;
    private UUID courseId;
    private UUID chapterId;
    private UUID cognitiveLevelId;
    private CognitiveLevelCode cognitiveLevelCode;
    private String cognitiveLevelName;
    private String content;
    @Builder.Default
    private RichContentFormat contentFormat = RichContentFormat.PLAIN_TEXT;
    private String explanation;
    @Builder.Default
    private RichContentFormat explanationFormat = RichContentFormat.PLAIN_TEXT;
    private Question.QuestionType questionType;
    private String codeRunnerContractJson;
    @Builder.Default
    private QuestionBankSourceType sourceType = QuestionBankSourceType.MANUAL;
    private UUID createdBy;
    private UUID validatedBy;
    private UUID supersedesBankQuestionId;
    private LocalDateTime validatedAt;
    @Builder.Default
    private long version = 0;
    @Builder.Default
    private List<QuestionBankAnswer> answers = new ArrayList<>();
    @Builder.Default
    private Set<UUID> learningOutcomeIds = new LinkedHashSet<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
