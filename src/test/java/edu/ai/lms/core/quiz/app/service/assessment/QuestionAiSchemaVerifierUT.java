package edu.ai.lms.core.quiz.app.service.assessment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionAiSchemaVerifierUT {
    private JdbcTemplate jdbc;
    private QuestionAiSchemaVerifier verifier;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        verifier = new QuestionAiSchemaVerifier(jdbc);
    }

    @Test
    void acceptsCompleteSchema() {
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(List.of());

        assertThatCode(verifier::verify).doesNotThrowAnyException();
    }

    @Test
    void reportsMissingTableAndMigration() {
        when(jdbc.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("table edu.question_ai_jobs"));

        assertThatThrownBy(verifier::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("table edu.question_ai_jobs")
                .hasMessageContaining(QuestionAiSchemaVerifier.MIGRATION)
                .hasMessageContaining("DATABASE_INITIALIZATION_OPTION=never");
    }

    @Test
    void reportsMissingArtifactColumn() {
        when(jdbc.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("column edu.rich_content_assets.ai_artifact_id"));

        assertThatThrownBy(verifier::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("column edu.rich_content_assets.ai_artifact_id");
    }

    @Test
    void reportsMissingActiveJobIndex() {
        when(jdbc.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("unique partial index edu.uq_question_ai_active_job"));

        assertThatThrownBy(verifier::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unique partial index edu.uq_question_ai_active_job");
    }
}
