package edu.ai.lms.core.quiz3;

import edu.ai.lms.core.quiz3.adapter.out.persistence.Quiz3RichContentAssetRepository;
import edu.ai.lms.core.quiz3.app.domain.Quiz3RichContentAsset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class Quiz3RichContentAssetRepositoryUT {
    @Test
    void writesOnlyToQuiz3AssetTable() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        Quiz3RichContentAssetRepository repository = new Quiz3RichContentAssetRepository(jdbc);
        repository.save(new Quiz3RichContentAsset(UUID.randomUUID(), UUID.randomUUID(), "quiz3/rich-content/a.svg",
                "image/svg+xml", 10, "0".repeat(64), null, UUID.randomUUID(), null, null));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class));
        assertThat(sql.getValue()).contains("edu.quiz3_rich_content_assets").doesNotContain("edu.rich_content_assets");
    }
}
