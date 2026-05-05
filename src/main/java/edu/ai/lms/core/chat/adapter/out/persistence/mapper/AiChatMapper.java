package edu.ai.lms.core.chat.adapter.out.persistence.mapper;

import edu.ai.lms.core.chat.app.domain.AiChatSession;
import edu.ai.lms.core.chat.app.domain.AiCourseChatMessage;
import edu.ai.lms.core.chat.app.service.dto.ChatSessionDto;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface AiChatMapper {

    // ==========================================
    // SESSION OPERATIONS
    // ==========================================

    @Insert("INSERT INTO ai_chat_sessions (id, user_id, course_id, title, is_pinned) " +
            "VALUES (#{id}, #{userId}, #{courseId}, #{title}, #{isPinned})")
    void insertSession(AiChatSession session);

    @Select("SELECT id, title, is_pinned AS isPinned, updated_at AS updatedAt " +
            "FROM ai_chat_sessions " +
            "WHERE user_id = #{userId} AND course_id = #{courseId} " +
            "ORDER BY is_pinned DESC, updated_at DESC")
    List<ChatSessionDto> selectSessionsByUserAndCourse(@Param("userId") UUID userId, @Param("courseId") UUID courseId);

    @Update("UPDATE ai_chat_sessions SET is_pinned = #{isPinned}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void updateSessionPin(@Param("id") UUID id, @Param("isPinned") boolean isPinned);

    @Update("UPDATE ai_chat_sessions SET title = #{title}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void updateSessionTitle(@Param("id") UUID id, @Param("title") String title);

    @Update("UPDATE ai_chat_sessions SET updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void touchSession(@Param("id") UUID id);

    @Delete("DELETE FROM ai_chat_sessions WHERE id = #{id}")
    void deleteSession(@Param("id") UUID id);

    // ==========================================
    // MESSAGE OPERATIONS
    // ==========================================

    @Insert("INSERT INTO ai_course_chat_messages (session_id, user_id, course_id, role, content) " +
            "VALUES (#{sessionId}, #{userId}, #{courseId}, #{role}, #{content})")
    void insertMessage(AiCourseChatMessage message);

    @Select("SELECT id, session_id AS sessionId, role, content, created_at AS createdAt " +
            "FROM ai_course_chat_messages " +
            "WHERE session_id = #{sessionId} " +
            "ORDER BY created_at ASC")
    List<AiCourseChatMessage> selectMessagesBySession(@Param("sessionId") UUID sessionId);

    @Select("SELECT COUNT(*) FROM ai_course_chat_messages WHERE session_id = #{sessionId}")
    int countMessagesBySession(@Param("sessionId") UUID sessionId);

    // ==========================================
    // MATERIAL VALIDATION (Source Selection - Anti-IDOR)
    // ==========================================

    @Select("<script>" +
            "SELECT COUNT(*) FROM materials " +
            "WHERE course_id = #{courseId} AND status = 'UPLOADED' AND id IN " +
            "<foreach item='id' collection='materialIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int countValidMaterials(@Param("courseId") UUID courseId, @Param("materialIds") List<UUID> materialIds);
}
