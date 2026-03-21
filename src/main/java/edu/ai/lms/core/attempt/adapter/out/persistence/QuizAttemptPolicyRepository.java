package edu.ai.lms.core.attempt.adapter.out.persistence;

import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
interface QuizAttemptPolicyRepository {

    Optional<QuizAttemptPolicy> getQuizAttemptPolicy(@Param("courseId") UUID courseId, @Param("quizId") UUID quizId);

    Optional<QuizAttemptPolicy> getQuizAttemptPolicyByQuizId(@Param("quizId") UUID quizId);

    List<QuestionAnswerKey> getQuestionAnswerKeys(@Param("quizId") UUID quizId);

    boolean isStudentInCourse(@Param("studentId") UUID studentId, @Param("courseId") UUID courseId);

    boolean quizHasTimer(@Param("quizId") UUID quizId);
}
