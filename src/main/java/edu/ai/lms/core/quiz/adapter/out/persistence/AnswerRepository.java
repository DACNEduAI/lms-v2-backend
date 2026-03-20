package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import edu.ai.lms.core.quiz.app.domain.Answer;

/**
 * Repository for answers. Answer is a weak entity with composite primary key
 * (answer_number, question_id), similar to how chapters use (chapter_number, course_id).
 * We use Long as the CrudRepository ID type (answer_number) but handle lookups
 * and deletes via custom queries that include question_id.
 */
@Mapper
interface AnswerRepository {

    List<Answer> getByQuestionId(@Param("questionId") UUID questionId);

    Optional<Answer> getByAnswerNumberAndQuestionId(
        @Param("answerNumber") Long answerNumber,
        @Param("questionId") UUID questionId);

    void deleteByQuestionId(@Param("questionId") UUID questionId);

    void deleteByAnswerNumberAndQuestionId(
            @Param("answerNumber") Long answerNumber,
            @Param("questionId") UUID questionId);

    /**
     * Custom INSERT that returns the auto-generated answer_number.
     * Used for creating new answers since answer_number is GENERATED ALWAYS AS IDENTITY.
     */
    Long insertAndReturnAnswerNumber(
            @Param("questionId") UUID questionId,
            @Param("content") String content,
            @Param("isCorrect") boolean isCorrect,
            @Param("explanation") String explanation);

    /**
     * Custom UPDATE by composite key (answer_number, question_id).
     * Standard save() doesn't work properly with composite keys that have auto-generated parts.
     */
    void updateByCompositeKey(
            @Param("answerNumber") Long answerNumber,
            @Param("questionId") UUID questionId,
            @Param("content") String content,
            @Param("isCorrect") boolean isCorrect,
            @Param("explanation") String explanation);
}
