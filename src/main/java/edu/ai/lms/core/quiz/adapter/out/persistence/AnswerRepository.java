package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.AnswerJdbcTable;

/**
 * Repository for answers. Answer is a weak entity with composite primary key
 * (answer_number, question_id), similar to how chapters use (chapter_number, course_id).
 * We use Long as the CrudRepository ID type (answer_number) but handle lookups
 * and deletes via custom queries that include question_id.
 */
@Repository
interface AnswerRepository extends CrudRepository<AnswerJdbcTable, Long> {

    @Query("SELECT * FROM answers WHERE question_id = :questionId ORDER BY answer_number")
    List<AnswerJdbcTable> findByQuestionId(@Param("questionId") UUID questionId);

    @Query("SELECT * FROM answers WHERE answer_number = :answerNumber AND question_id = :questionId")
    Optional<AnswerJdbcTable> findByAnswerNumberAndQuestionId(
            @Param("answerNumber") Long answerNumber,
            @Param("questionId") UUID questionId);

    @Modifying
    @Query("DELETE FROM answers WHERE question_id = :questionId")
    void deleteByQuestionId(@Param("questionId") UUID questionId);

    @Modifying
    @Query("DELETE FROM answers WHERE answer_number = :answerNumber AND question_id = :questionId")
    void deleteByAnswerNumberAndQuestionId(
            @Param("answerNumber") Long answerNumber,
            @Param("questionId") UUID questionId);

    /**
     * Custom INSERT that returns the auto-generated answer_number.
     * Used for creating new answers since answer_number is GENERATED ALWAYS AS IDENTITY.
     */
    @Query("INSERT INTO answers (question_id, content, is_correct, explanation, created_at, updated_at) " +
           "VALUES (:questionId, :content, :isCorrect, :explanation, NOW(), NOW()) " +
           "RETURNING answer_number")
    Long insertAndReturnAnswerNumber(
            @Param("questionId") UUID questionId,
            @Param("content") String content,
            @Param("isCorrect") boolean isCorrect,
            @Param("explanation") String explanation);

    /**
     * Custom UPDATE by composite key (answer_number, question_id).
     * Standard save() doesn't work properly with composite keys that have auto-generated parts.
     */
    @Modifying
    @Query("UPDATE answers SET content = :content, is_correct = :isCorrect, explanation = :explanation, updated_at = NOW() " +
           "WHERE answer_number = :answerNumber AND question_id = :questionId")
    void updateByCompositeKey(
            @Param("answerNumber") Long answerNumber,
            @Param("questionId") UUID questionId,
            @Param("content") String content,
            @Param("isCorrect") boolean isCorrect,
            @Param("explanation") String explanation);
}
