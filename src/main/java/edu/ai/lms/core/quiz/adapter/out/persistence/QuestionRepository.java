package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.QuestionJdbcTable;

@Repository
interface QuestionRepository extends CrudRepository<QuestionJdbcTable, UUID> {

    @Query("SELECT * FROM questions WHERE quiz_id = :quizId ORDER BY created_at")
    List<QuestionJdbcTable> findByQuizId(@Param("quizId") UUID quizId);
}
