package edu.ai.lms.core.quiz.app.port.out;

import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItem;

public interface LoadQuestionBankItemPort {
    Optional<QuestionBankItem> findByCourseIdAndId(UUID courseId, UUID itemId);
}
