package edu.ai.lms.core.quiz3.adapter.out.persistence;

import edu.ai.lms.core.quiz3.app.domain.*;
import edu.ai.lms.core.quiz3.app.service.*;
import edu.ai.lms.core.quiz3.adapter.out.persistence.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/** Persistence boundary for Quiz3 embedded-image metadata. */
@Repository
@RequiredArgsConstructor
public class Quiz3RichContentAssetRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public void save(Quiz3RichContentAsset asset) {
        jdbc.update("""
                INSERT INTO edu.quiz3_rich_content_assets
                    (id, question_id, storage_key, media_type, byte_size, sha256, source_asset_id, ai_artifact_id)
                VALUES (:id, :questionId, :storageKey, :mediaType, :byteSize, :sha256, :sourceAssetId, :aiArtifactId)
                """, parameters(asset));
    }

    public void saveAll(Collection<Quiz3RichContentAsset> assets) {
        if (assets != null) assets.forEach(this::save);
    }

    public List<Quiz3RichContentAsset> findByQuestionId(UUID questionId) {
        return jdbc.query("SELECT * FROM edu.quiz3_rich_content_assets WHERE question_id=:questionId ORDER BY created_at,id",
                Map.of("questionId", questionId), (rs, row) -> new Quiz3RichContentAsset(
                        rs.getObject("id", UUID.class), rs.getObject("question_id", UUID.class),
                        rs.getString("storage_key"), rs.getString("media_type"), rs.getLong("byte_size"),
                        rs.getString("sha256"), rs.getObject("source_asset_id", UUID.class), rs.getObject("ai_artifact_id", UUID.class),
                        rs.getObject("created_at", LocalDateTime.class), rs.getObject("updated_at", LocalDateTime.class)));
    }

    public List<String> storageKeysForArtifactIds(Collection<UUID> artifactIds) {
        if (artifactIds == null || artifactIds.isEmpty()) return List.of();
        return jdbc.query("SELECT storage_key FROM edu.quiz3_rich_content_assets WHERE ai_artifact_id IN (:artifactIds)",
                new MapSqlParameterSource("artifactIds", artifactIds), (rs, row) -> rs.getString(1));
    }

    public void retainForQuestion(UUID questionId, Set<String> storageKeys) {
        if (storageKeys == null || storageKeys.isEmpty()) {
            jdbc.update("DELETE FROM edu.quiz3_rich_content_assets WHERE question_id=:questionId", Map.of("questionId", questionId));
            return;
        }
        jdbc.update("DELETE FROM edu.quiz3_rich_content_assets WHERE question_id=:questionId AND storage_key NOT IN (:storageKeys)",
                new MapSqlParameterSource().addValue("questionId", questionId).addValue("storageKeys", storageKeys));
    }

    private MapSqlParameterSource parameters(Quiz3RichContentAsset asset) {
        return new MapSqlParameterSource()
                .addValue("id", asset.id() == null ? UUID.randomUUID() : asset.id())
                .addValue("questionId", asset.questionId()).addValue("storageKey", asset.storageKey())
                .addValue("mediaType", asset.mediaType()).addValue("byteSize", asset.byteSize())
                .addValue("sha256", asset.sha256()).addValue("sourceAssetId", asset.sourceAssetId()).addValue("aiArtifactId", asset.aiArtifactId());
    }
}
