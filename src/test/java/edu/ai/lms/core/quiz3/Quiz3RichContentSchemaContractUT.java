package edu.ai.lms.core.quiz3;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class Quiz3RichContentSchemaContractUT {
    @Test
    void resetSchemaKeepsQuiz3AssetFkOutOfQuiz2Tables() throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/schema.sql"));
        int create = schema.indexOf("CREATE TABLE IF NOT EXISTS quiz3_rich_content_assets");
        assertThat(create).isPositive();
        String definition = schema.substring(create, schema.indexOf("CREATE TABLE IF NOT EXISTS quiz3_question_ai_change_sets", create));
        assertThat(definition).contains("REFERENCES quiz3_question_ai_artifacts(id) ON DELETE CASCADE")
                .contains("REFERENCES questions(id) ON DELETE CASCADE")
                .doesNotContain("REFERENCES question_ai_artifacts");
        assertThat(schema.indexOf("quiz3_rich_content_assets,")).isLessThan(schema.indexOf("quiz3_question_ai_artifacts,"));
    }
}
