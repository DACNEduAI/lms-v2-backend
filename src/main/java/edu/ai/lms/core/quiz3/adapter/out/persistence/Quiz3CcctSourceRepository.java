package edu.ai.lms.core.quiz3.adapter.out.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class Quiz3CcctSourceRepository {
    private final NamedParameterJdbcTemplate jdbc;

    @Transactional
    public UUID cloneCcct(UUID courseId, UUID sourceId, UUID actorId) {
        Map<String, Object> source = jdbc.queryForMap("""
                SELECT name, template_title, format_version, structured_current, last_mode, markdown_content
                FROM edu.ccct_profiles WHERE course_id = :courseId AND id = :sourceId
                """, Map.of("courseId", courseId, "sourceId", sourceId));
        UUID cloneId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.ccct_profiles
                    (id, course_id, name, template_title, status, version, format_version,
                     structured_current, last_mode, markdown_content, created_by)
                VALUES (:id, :courseId, :name, :template_title, 'DRAFT', 0, :format_version,
                        :structured_current, :last_mode, :markdown_content, :actorId)
                """, new MapSqlParameterSource(source)
                .addValue("id", cloneId).addValue("courseId", courseId)
                .addValue("name", source.get("name") + "-Copy").addValue("actorId", actorId));

        Map<UUID, UUID> chapterIds = new HashMap<>();
        jdbc.query("""
                SELECT id, course_chapter_id, title, custom_content, order_index
                FROM edu.ccct_chapters WHERE profile_id = :sourceId ORDER BY order_index
                """, Map.of("sourceId", sourceId), rs -> {
            UUID oldId = rs.getObject("id", UUID.class);
            UUID newId = UUID.randomUUID();
            chapterIds.put(oldId, newId);
            jdbc.update("""
                    INSERT INTO edu.ccct_chapters
                        (id, profile_id, course_chapter_id, title, custom_content, order_index)
                    VALUES (:id, :profileId, :courseChapterId, :title, :customContent, :orderIndex)
                    """, new MapSqlParameterSource()
                    .addValue("id", newId).addValue("profileId", cloneId)
                    .addValue("courseChapterId", rs.getObject("course_chapter_id", UUID.class))
                    .addValue("title", rs.getString("title"))
                    .addValue("customContent", rs.getString("custom_content"))
                    .addValue("orderIndex", rs.getInt("order_index")));
        });

        Map<UUID, UUID> topicIds = new HashMap<>();
        for (Map.Entry<UUID, UUID> entry : chapterIds.entrySet()) {
            jdbc.query("""
                    SELECT id, title, order_index FROM edu.ccct_topics
                    WHERE chapter_id = :chapterId ORDER BY order_index
                    """, Map.of("chapterId", entry.getKey()), rs -> {
                UUID newId = UUID.randomUUID();
                topicIds.put(rs.getObject("id", UUID.class), newId);
                jdbc.update("""
                        INSERT INTO edu.ccct_topics (id, chapter_id, title, order_index)
                        VALUES (:id, :chapterId, :title, :orderIndex)
                        """, new MapSqlParameterSource().addValue("id", newId)
                        .addValue("chapterId", entry.getValue()).addValue("title", rs.getString("title"))
                        .addValue("orderIndex", rs.getInt("order_index")));
            });
        }
        for (Map.Entry<UUID, UUID> entry : topicIds.entrySet()) {
            jdbc.query("""
                    SELECT content, coverage_status, note, order_index FROM edu.ccct_items
                    WHERE topic_id = :topicId ORDER BY order_index
                    """, Map.of("topicId", entry.getKey()), rs -> {
                jdbc.update("""
                        INSERT INTO edu.ccct_items (id, topic_id, content, coverage_status, note, order_index)
                        VALUES (:id, :topicId, :content, :coverageStatus, :note, :orderIndex)
                        """, new MapSqlParameterSource().addValue("id", UUID.randomUUID())
                        .addValue("topicId", entry.getValue()).addValue("content", rs.getString("content"))
                        .addValue("coverageStatus", rs.getString("coverage_status"))
                        .addValue("note", rs.getString("note")).addValue("orderIndex", rs.getInt("order_index")));
            });
        }
        return cloneId;
    }

    @Transactional
    public UUID createCcctDraft(UUID courseId, String name, UUID actorId) {
        List<Map<String, Object>> chapters = jdbc.queryForList("""
                SELECT chapter_id, title FROM edu.chapters
                WHERE course_id = :courseId ORDER BY chapter_id
                """, Map.of("courseId", courseId));
        if (chapters.isEmpty()) {
            throw new IllegalStateException("Create at least one Course Chapter before creating a CCCT Draft");
        }
        UUID profileId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.ccct_profiles
                    (id, course_id, name, template_title, status, version, format_version,
                     structured_current, last_mode, markdown_content, created_by)
                VALUES (:id, :courseId, :name, 'Course Content Coverage Template', 'DRAFT', 0, 2,
                        false, 'CUSTOM', '', :actorId)
                """, new MapSqlParameterSource().addValue("id", profileId)
                .addValue("courseId", courseId).addValue("name", name).addValue("actorId", actorId));
        for (int index = 0; index < chapters.size(); index++) {
            Map<String, Object> chapter = chapters.get(index);
            jdbc.update("""
                    INSERT INTO edu.ccct_chapters
                        (id, profile_id, course_chapter_id, title, custom_content, order_index)
                    VALUES (:id, :profileId, :courseChapterId, :title, '', :orderIndex)
                    """, new MapSqlParameterSource().addValue("id", UUID.randomUUID())
                    .addValue("profileId", profileId).addValue("courseChapterId", chapter.get("chapter_id"))
                    .addValue("title", chapter.get("title")).addValue("orderIndex", index));
        }
        return profileId;
    }
}
