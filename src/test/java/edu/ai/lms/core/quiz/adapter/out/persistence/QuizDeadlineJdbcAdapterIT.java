package edu.ai.lms.core.quiz.adapter.out.persistence;

import edu.ai.lms.config.IntegrationTestConfiguration; // File setup DB ảo của dự án
import edu.ai.lms.core.quiz.app.domain.PendingQuizTarget;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QuizDeadlineJdbcAdapterIT extends IntegrationTestConfiguration {

    @Autowired
    private QuizDeadlineJdbcAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldFindStudentsWhoHaveNotFinishedQuiz() {
        // 1. Chuẩn bị Dữ liệu thật trong DB Ảo (Tạo User, Course, Quiz, Enrollment)
        UUID studentId1 = UUID.randomUUID(); // Sẽ chưa làm bài
        UUID studentId2 = UUID.randomUUID(); // Đã làm bài xong
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueTime = now.plusHours(23).plusMinutes(30); // Nằm trong khoảng 23-24h

        // Insert Users & Course
        jdbcTemplate.update("INSERT INTO users (id, email, password) VALUES (?, 'sv1@test.com', '123'), (?, 'sv2@test.com', '123')", studentId1, studentId2);
        jdbcTemplate.update("INSERT INTO students (user_id, student_id, enrollment_year) VALUES (?, 'SV01', 2026), (?, 'SV02', 2026)", studentId1, studentId2);
        jdbcTemplate.update("INSERT INTO courses (id, course_name, course_code, active, start_date, end_date) VALUES (?, 'Course', 'C01', true, now(), now())", courseId);
        
        // Cả 2 SV đều đăng ký khóa học
        jdbcTemplate.update("INSERT INTO enrollments (course_id, user_id) VALUES (?, ?), (?, ?)", courseId, studentId1, courseId, studentId2);

        // Tạo 1 Quiz sắp hết hạn. Note: Quizzes DO NOT have active column.
        jdbcTemplate.update("INSERT INTO quizzes (id, quiz_title, course_id, due_time, display) VALUES (?, 'Quiz Test', ?, ?, true)", quizId, courseId, dueTime);

        // SV 2 đã nộp bài (FINISHED)
        jdbcTemplate.update("INSERT INTO attempts (id, student_id, quiz_id, status) VALUES (gen_random_uuid(), ?, ?, 'FINISHED')", studentId2, quizId);

        // 2. Chạy hàm (Adapter chạy câu raw SQL)
        List<PendingQuizTarget> targets = adapter.findStudentsWithApproachingDeadlines(
                now.plusHours(23), 
                now.plusHours(24)
        );

        // 3. Kiểm chứng
        assertThat(targets).hasSize(1); // Chỉ được phép trả về 1 người
        assertThat(targets.get(0).studentId()).isEqualTo(studentId1); // Người đó phải là SV 1 (chưa làm bài)
        assertThat(targets.get(0).quizId()).isEqualTo(quizId);
    }
}
