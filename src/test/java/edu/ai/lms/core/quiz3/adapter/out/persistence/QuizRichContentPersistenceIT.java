package edu.ai.lms.core.quiz3.adapter.out.persistence;

import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.quiz3.app.domain.richcontent.RichContentFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QuizRichContentPersistenceIT extends IntegrationTestConfiguration {

    @Autowired private QuizPersistenceAdapter quizzes;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void loadsPersistedTiptapFormatsForQuestionAndAnswers() {
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        jdbc.update("""
                INSERT INTO edu.courses (id, course_name, course_code, active, start_date, end_date)
                VALUES (?, 'Data structures', ?, true, NOW(), NOW())
                """, courseId, "DS-" + courseId);
        jdbc.update("""
                INSERT INTO edu.quizzes (id, quiz_title, course_id, lifecycle_status, source_type)
                VALUES (?, 'Rich content', ?, 'PUBLISHED', 'AI_GENERATED')
                """, quizId, courseId);
        jdbc.update("""
                INSERT INTO edu.questions
                    (id, content, content_format, explanation, explanation_format, type, quiz_id, review_status, source_type)
                VALUES (?, '<p>Question</p>', 'TIPTAP_HTML', '<p>Explanation</p>', 'TIPTAP_HTML',
                    'MULTIPLE_CHOICE', ?, 'APPROVED', 'AI_GENERATED')
                """, questionId, quizId);
        jdbc.update("""
                INSERT INTO edu.answers
                    (content, content_format, is_correct, explanation, explanation_format, question_id)
                VALUES ('<p>Answer</p>', 'TIPTAP_HTML', true, '<p>Why</p>', 'TIPTAP_HTML', ?)
                """, questionId);

        var question = quizzes.getByIdWithQuestionsAndAnswers(courseId, quizId)
                .orElseThrow()
                .getQuestions()
                .getFirst();
        var answer = question.getAnswers().getFirst();

        assertThat(question.getContentFormat()).isEqualTo(RichContentFormat.TIPTAP_HTML);
        assertThat(question.getExplanationFormat()).isEqualTo(RichContentFormat.TIPTAP_HTML);
        assertThat(answer.getContentFormat()).isEqualTo(RichContentFormat.TIPTAP_HTML);
        assertThat(answer.getExplanationFormat()).isEqualTo(RichContentFormat.TIPTAP_HTML);
    }
}
