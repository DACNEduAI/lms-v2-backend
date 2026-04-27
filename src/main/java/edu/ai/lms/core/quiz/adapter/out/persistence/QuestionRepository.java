package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import edu.ai.lms.core.quiz.app.domain.Question;

@Mapper
interface QuestionRepository {

    int insert(Question entity);

    int update(Question entity);

    default Question save(Question entity) {
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

    void deleteById(@Param("id") UUID id);

    Optional<Question> getById(@Param("questionId") UUID questionId);

    List<Question> getByQuizId(@Param("quizId") UUID quizId);

    List<Question.Image> getImagesByQuestionId(@Param("questionId") UUID questionId);

    void deleteImagesByQuestionId(@Param("questionId") UUID questionId);

    void insertImage(@Param("questionId") UUID questionId,
                     @Param("imageName") String imageName,
                     @Param("url") String url);
}
