package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.port.out.DeleteAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.LoadAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import lombok.RequiredArgsConstructor;

/**
 * Persistence adapter for Answer entity.
 * Answer has a composite primary key (answer_number, question_id) where answer_number
 * is auto-generated (GENERATED ALWAYS AS IDENTITY), following the same pattern as
 * ChapterPersistenceAdapter.
 */
@Component
@RequiredArgsConstructor
class AnswerPersistenceAdapter implements SaveAnswerPort, LoadAnswerPort, DeleteAnswerPort {

    private final AnswerRepository answerRepository;

    @Override
    public Answer save(Answer answer) {
        UUID questionId = answer.getQuestionId();
        boolean isUpdate = answer.getAnswerNumber() != null;

        if (isUpdate) {
            // UPDATE existing answer using custom method for composite key
            answerRepository.updateByCompositeKey(
                    answer.getAnswerNumber(),
                    questionId,
                    answer.getContent(),
                    answer.isCorrect(),
                    answer.getExplanation());

            // Re-fetch the updated answer
                Answer updatedEntity = answerRepository
                    .getByAnswerNumberAndQuestionId(answer.getAnswerNumber(), questionId)
                    .orElseThrow(() -> new RuntimeException("Answer not found after update"));
                return updatedEntity;
        } else {
            // INSERT new answer using custom method that returns generated answer_number
            Long generatedAnswerNumber = answerRepository.insertAndReturnAnswerNumber(
                    questionId,
                    answer.getContent(),
                    answer.isCorrect(),
                    answer.getExplanation());

            // Fetch the newly created answer
                Answer savedEntity = answerRepository
                    .getByAnswerNumberAndQuestionId(generatedAnswerNumber, questionId)
                    .orElseThrow(() -> new RuntimeException("Answer not found after insert"));
                return savedEntity;
        }
    }

    @Override
    public Optional<Answer> getByKey(UUID questionId, Long answerNumber) {
        return answerRepository.getByAnswerNumberAndQuestionId(answerNumber, questionId);
    }

    @Override
    public List<Answer> getByQuestionId(UUID questionId) {
        return answerRepository.getByQuestionId(questionId).stream().collect(Collectors.toList());
    }

    @Override
    public void deleteByKey(UUID questionId, Long answerNumber) {
        answerRepository.deleteByAnswerNumberAndQuestionId(answerNumber, questionId);
    }
}
