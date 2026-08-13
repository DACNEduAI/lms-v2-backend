package edu.ai.lms.core.quiz.app.port.in.command;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelCode;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;

public record QuestionBankItemCommand(
        UUID chapterId,
        String content,
        RichContentFormat contentFormat,
        String explanation,
        RichContentFormat explanationFormat,
        Question.QuestionType questionType,
        CognitiveLevelCode cognitiveLevelCode,
        List<Answer> answers,
        Set<UUID> learningOutcomeIds) {

    public QuestionBankItemCommand(UUID chapterId, String content, Question.QuestionType questionType,
            CognitiveLevelCode cognitiveLevelCode, List<Answer> answers, Set<UUID> learningOutcomeIds) {
        this(chapterId, content, RichContentFormat.PLAIN_TEXT, null, RichContentFormat.PLAIN_TEXT,
                questionType, cognitiveLevelCode, answers, learningOutcomeIds);
    }

    public record Answer(String content, RichContentFormat contentFormat, boolean correct) {
        public Answer(String content, boolean correct, String ignoredExplanation) {
            this(content, RichContentFormat.PLAIN_TEXT, correct);
        }

        public Answer(String content, RichContentFormat contentFormat, boolean correct,
                String ignoredExplanation, RichContentFormat ignoredExplanationFormat) {
            this(content, contentFormat, correct);
        }
    }
}
