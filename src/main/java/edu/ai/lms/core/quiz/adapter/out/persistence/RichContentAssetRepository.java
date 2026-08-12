package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentAsset;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RichContentAssetRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public void save(RichContentAsset asset) {
        jdbc.update("""
                INSERT INTO edu.rich_content_assets
                    (id, question_draft_id, question_bank_question_id, question_id,
                     storage_key, media_type, byte_size, sha256)
                VALUES (:id, :draftId, :bankId, :questionId, :storageKey, :mediaType, :byteSize, :sha256)
                """, new MapSqlParameterSource()
                .addValue("id", asset.id() == null ? UUID.randomUUID() : asset.id())
                .addValue("draftId", asset.questionDraftId())
                .addValue("bankId", asset.questionBankQuestionId())
                .addValue("questionId", asset.questionId())
                .addValue("storageKey", asset.storageKey())
                .addValue("mediaType", asset.mediaType())
                .addValue("byteSize", asset.byteSize())
                .addValue("sha256", asset.sha256()));
    }

    public void saveAll(List<RichContentAsset> assets) {
        assets.forEach(this::save);
    }

    public void copyDraftAssetsToBank(UUID draftId, UUID bankId) {
        copy("question_draft_id", draftId, "question_bank_question_id", bankId);
    }

    public void copyBankAssetsToDraft(UUID bankId, UUID draftId) {
        copy("question_bank_question_id", bankId, "question_draft_id", draftId);
    }

    public void copyBankAssetsToQuestion(UUID bankId, UUID questionId) {
        copy("question_bank_question_id", bankId, "question_id", questionId);
    }

    public List<RichContentAsset> findByQuestionDraftId(UUID draftId) {
        return find("question_draft_id", draftId);
    }

    public List<RichContentAsset> findByQuestionBankQuestionId(UUID bankQuestionId) {
        return find("question_bank_question_id", bankQuestionId);
    }

    public List<RichContentAsset> findByQuestionId(UUID questionId) {
        return find("question_id", questionId);
    }

    private List<RichContentAsset> find(String ownerColumn, UUID ownerId) {
        return jdbc.query("SELECT * FROM edu.rich_content_assets WHERE " + ownerColumn + " = :ownerId ORDER BY created_at, id",
                Map.of("ownerId", ownerId), (rs, rowNum) -> new RichContentAsset(
                        rs.getObject("id", UUID.class), rs.getObject("question_draft_id", UUID.class),
                        rs.getObject("question_bank_question_id", UUID.class), rs.getObject("question_id", UUID.class),
                        rs.getString("storage_key"), rs.getString("media_type"), rs.getLong("byte_size"),
                        rs.getString("sha256"), rs.getObject("created_at", LocalDateTime.class),
                        rs.getObject("updated_at", LocalDateTime.class)));
    }

    private void copy(String sourceColumn, UUID sourceId, String targetColumn, UUID targetId) {
        jdbc.update("""
                INSERT INTO edu.rich_content_assets
                    (id, %s, storage_key, media_type, byte_size, sha256)
                SELECT gen_random_uuid(), :targetId, storage_key, media_type, byte_size, sha256
                FROM edu.rich_content_assets WHERE %s = :sourceId
                """.formatted(targetColumn, sourceColumn), Map.of("sourceId", sourceId, "targetId", targetId));
    }
}
