package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import edu.ai.lms.core.quiz.app.domain.Quiz;

@Mapper
interface QuizRepository {

    int insert(Quiz entity);

    int update(Quiz entity);

    default Quiz save(Quiz entity) {
        if (entity.getId() == null) {
            entity.setId(java.util.UUID.randomUUID());
            insert(entity);
        } else {
            int updated = update(entity);
            if (updated == 0) {
                insert(entity);
            }
        }
        return getById(entity.getId()).orElseThrow();
    }

    void deleteById(@Param("quizId") UUID quizId);

    Optional<Quiz> getById(@Param("quizId") UUID quizId);

    Optional<Quiz> getByIdAndCourseId(@Param("quizId") UUID quizId, @Param("courseId") UUID courseId);

    List<Quiz> getAllByCourseId(@Param("courseId") UUID courseId);

    List<Quiz> getByCourseIdAndTeacherId(
            @Param("courseId") UUID courseId,
            @Param("teacherId") UUID teacherId);
}
