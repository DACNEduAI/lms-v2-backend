package edu.ai.lms.core.quiz3.app.service;

import edu.ai.lms.core.quiz3.app.domain.*;
import edu.ai.lms.core.quiz3.app.service.*;
import edu.ai.lms.core.quiz3.adapter.out.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz3.api.QuizReleaseApi;
import lombok.RequiredArgsConstructor;

/** Bridges Quiz3's variant workflow to the shared immutable release model. */
@Component
@RequiredArgsConstructor
class Quiz3ReleaseCoordinator {
    private final JdbcTemplate jdbc;
    private final QuizReleaseApi releases;

    void publish(List<UUID> quizIds, UUID publishedBy, LocalDateTime publishedAt) {
        for (UUID quizId : quizIds) {
            Long quizVersion = jdbc.queryForObject(
                    "SELECT version FROM edu.quizzes WHERE id=?",
                    Long.class,
                    quizId);
            if (quizVersion == null) {
                throw new IllegalStateException("Published Quiz3 variant has no quiz version: " + quizId);
            }
            releases.publish(quizId, quizVersion, publishedBy, publishedAt);
        }
    }

    void deactivate(List<UUID> quizIds) {
        quizIds.forEach(releases::deactivate);
    }
}
