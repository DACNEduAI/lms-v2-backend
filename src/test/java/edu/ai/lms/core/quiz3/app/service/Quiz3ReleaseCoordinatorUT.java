package edu.ai.lms.core.quiz3.app.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import edu.ai.lms.core.quiz3.api.QuizReleaseApi;

@ExtendWith(MockitoExtension.class)
class Quiz3ReleaseCoordinatorUT {
    @Mock private JdbcTemplate jdbc;
    @Mock private QuizReleaseApi releases;
    @InjectMocks private Quiz3ReleaseCoordinator coordinator;

    @Test
    void publishesEveryVariantAtItsPersistedQuizVersion() {
        UUID firstQuiz = UUID.randomUUID();
        UUID secondQuiz = UUID.randomUUID();
        UUID teacher = UUID.randomUUID();
        LocalDateTime publishedAt = LocalDateTime.now();
        when(jdbc.queryForObject("SELECT version FROM edu.quizzes WHERE id=?", Long.class, firstQuiz))
                .thenReturn(4L);
        when(jdbc.queryForObject("SELECT version FROM edu.quizzes WHERE id=?", Long.class, secondQuiz))
                .thenReturn(9L);

        coordinator.publish(List.of(firstQuiz, secondQuiz), teacher, publishedAt);

        verify(releases).publish(firstQuiz, 4L, teacher, publishedAt);
        verify(releases).publish(secondQuiz, 9L, teacher, publishedAt);
    }

    @Test
    void deactivatesEveryVariantRelease() {
        UUID firstQuiz = UUID.randomUUID();
        UUID secondQuiz = UUID.randomUUID();

        coordinator.deactivate(List.of(firstQuiz, secondQuiz));

        verify(releases).deactivate(firstQuiz);
        verify(releases).deactivate(secondQuiz);
    }
}
