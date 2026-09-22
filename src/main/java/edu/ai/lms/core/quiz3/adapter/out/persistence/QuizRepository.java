package edu.ai.lms.core.quiz3.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import edu.ai.lms.core.quiz3.app.domain.Quiz;
import edu.ai.lms.core.quiz3.app.domain.QuizLifecycleStatus;

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

    List<Quiz> getTeacherCreatedByCourseId(@Param("courseId") UUID courseId);

    List<Quiz> getStudentCreatedByCourseIdAndStudentId(@Param("courseId") UUID courseId, @Param("studentId") UUID studentId);

    int touchContent(
            @Param("courseId") UUID courseId,
            @Param("quizId") UUID quizId,
            @Param("expectedVersion") long expectedVersion,
            @Param("expectedStatus") QuizLifecycleStatus expectedStatus);

    int transitionLifecycle(
            @Param("courseId") UUID courseId,
            @Param("quizId") UUID quizId,
            @Param("expectedVersion") long expectedVersion,
            @Param("expectedStatus") QuizLifecycleStatus expectedStatus,
            @Param("nextStatus") QuizLifecycleStatus nextStatus,
            @Param("display") boolean display,
            @Param("publishedBy") UUID publishedBy,
            @Param("publishedAt") java.time.LocalDateTime publishedAt);
}
