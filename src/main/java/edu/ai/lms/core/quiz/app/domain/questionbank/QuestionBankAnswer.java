package edu.ai.lms.core.quiz.app.domain.questionbank;

import java.time.LocalDateTime;
import java.util.UUID;

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
public class QuestionBankAnswer {
    private UUID id;
    private UUID questionId;
    private String content;
    @Builder.Default
    private RichContentFormat contentFormat = RichContentFormat.PLAIN_TEXT;
    private boolean correct;
    private String explanation;
    @Builder.Default
    private RichContentFormat explanationFormat = RichContentFormat.PLAIN_TEXT;
    private int orderIndex;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
