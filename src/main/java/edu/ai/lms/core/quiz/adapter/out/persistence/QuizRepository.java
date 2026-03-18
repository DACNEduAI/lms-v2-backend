package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.QuizJdbcTable;

@Repository
interface QuizRepository extends CrudRepository<QuizJdbcTable, UUID> {

    @Query("SELECT * FROM quizzes WHERE id = :quizId")
    Optional<QuizJdbcTable> getById(@Param("quizId") UUID quizId);

    @Query("SELECT * FROM quizzes WHERE id = :quizId AND course_id = :courseId")
    Optional<QuizJdbcTable> getByIdAndCourseId(@Param("quizId") UUID quizId, @Param("courseId") UUID courseId);

    @Query("SELECT * FROM quizzes WHERE course_id = :courseId ORDER BY created_at DESC")
    List<QuizJdbcTable> getAllByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT * FROM quizzes WHERE course_id = :courseId AND teacher_id = :teacherId ORDER BY created_at DESC")
    List<QuizJdbcTable> getByCourseIdAndTeacherId(
            @Param("courseId") UUID courseId,
            @Param("teacherId") UUID teacherId);
}
