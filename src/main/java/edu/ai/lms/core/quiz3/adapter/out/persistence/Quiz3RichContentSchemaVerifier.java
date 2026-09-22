package edu.ai.lms.core.quiz3.adapter.out.persistence;

import edu.ai.lms.core.quiz3.app.domain.*;
import edu.ai.lms.core.quiz3.app.service.*;
import edu.ai.lms.core.quiz3.adapter.out.persistence.*;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/** Fails fast instead of misreporting a missing Quiz3 asset table as a Chromium error. */
@Component
@RequiredArgsConstructor
class Quiz3RichContentSchemaVerifier {
    static final String MIGRATION = "src/main/resources/db/manual/V20260831_01__quiz3_rich_content_assets.sql";

    private static final String MISSING_OBJECTS_SQL = """
            SELECT object_name
            FROM (VALUES
                ('table edu.quiz3_rich_content_assets', to_regclass('edu.quiz3_rich_content_assets') IS NOT NULL),
                ('index edu.idx_quiz3_rich_content_assets_question', to_regclass('edu.idx_quiz3_rich_content_assets_question') IS NOT NULL),
                ('index edu.idx_quiz3_rich_content_assets_ai_artifact', to_regclass('edu.idx_quiz3_rich_content_assets_ai_artifact') IS NOT NULL)
            ) required(object_name, present)
            WHERE NOT present
            ORDER BY object_name
            """;

    private final JdbcTemplate jdbc;

    @PostConstruct
    void verify() {
        List<String> missing = jdbc.queryForList(MISSING_OBJECTS_SQL, String.class);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Quiz3 rich-content database migration is missing: "
                    + String.join(", ", missing) + ". Stop the backend and apply " + MIGRATION
                    + " while keeping DATABASE_INITIALIZATION_OPTION=never.");
        }
    }
}
