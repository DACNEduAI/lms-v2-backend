package edu.ai.lms.core.quiz.adapter.out.persistence;

import edu.ai.lms.core.quiz.app.domain.PendingQuizTarget;
import edu.ai.lms.core.quiz.app.port.out.LoadPendingQuizDeadlinePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class QuizDeadlineJdbcAdapter implements LoadPendingQuizDeadlinePort {

    private final JdbcClient jdbcClient;

    @Override
    public List<PendingQuizTarget> findStudentsWithApproachingDeadlines(LocalDateTime fromTime, LocalDateTime toTime) {
        String sql = """
            SELECT 
                e.user_id AS student_id, 
                q.id AS quiz_id, 
                q.quiz_title, 
                q.course_id
            FROM quizzes q
            INNER JOIN enrollments e ON q.course_id = e.course_id
            LEFT JOIN attempts a ON a.quiz_id = q.id 
                                AND a.student_id = e.user_id 
                                AND a.status = 'FINISHED'
            WHERE q.due_time >= :fromTime AND q.due_time <= :toTime
              AND q.display = true
              AND a.id IS NULL
            """;

        return jdbcClient.sql(sql)
                .param("fromTime", fromTime)
                .param("toTime", toTime)
                .query((rs, rowNum) -> new PendingQuizTarget(
                        rs.getObject("student_id", UUID.class),
                        rs.getObject("quiz_id", UUID.class),
                        rs.getString("quiz_title"),
                        rs.getObject("course_id", UUID.class)
                ))
                .list();
    }
}
