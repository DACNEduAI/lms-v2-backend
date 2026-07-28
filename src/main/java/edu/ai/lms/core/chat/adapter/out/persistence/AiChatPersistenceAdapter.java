package edu.ai.lms.core.chat.adapter.out.persistence;

import edu.ai.lms.core.chat.adapter.out.persistence.mapper.AiChatMapper;
import edu.ai.lms.core.chat.app.domain.AiChatSession;
import edu.ai.lms.core.chat.app.domain.AiCourseChatMessage;
import edu.ai.lms.core.chat.app.port.out.persistence.AiChatPersistencePort;
import edu.ai.lms.core.chat.adapter.in.http.request.ChatSessionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiChatPersistenceAdapter implements AiChatPersistencePort {

    private final AiChatMapper aiChatMapper;

    // ==========================================
    // SESSION OPERATIONS
    // ==========================================

    @Override
    public void saveSession(AiChatSession session) {
        aiChatMapper.insertSession(session);
    }

    @Override
    public List<ChatSessionDto> getSessionsByUserAndCourse(UUID userId, UUID courseId) {
        return aiChatMapper.selectSessionsByUserAndCourse(userId, courseId);
    }

    @Override
    public void updateSessionPin(UUID sessionId, boolean isPinned) {
        aiChatMapper.updateSessionPin(sessionId, isPinned);
    }

    @Override
    public void updateSessionTitle(UUID sessionId, String title) {
        aiChatMapper.updateSessionTitle(sessionId, title);
    }

    @Override
    public void touchSession(UUID sessionId) {
        aiChatMapper.touchSession(sessionId);
    }

    @Override
    public void deleteSession(UUID sessionId) {
        aiChatMapper.deleteSession(sessionId);
    }

    // ==========================================
    // MESSAGE OPERATIONS
    // ==========================================

    @Override
    public void saveMessage(AiCourseChatMessage message) {
        aiChatMapper.insertMessage(message);
    }

    @Override
    public List<AiCourseChatMessage> getMessagesBySession(UUID sessionId) {
        return aiChatMapper.selectMessagesBySession(sessionId);
    }

    @Override
    public int countMessagesBySession(UUID sessionId) {
        return aiChatMapper.countMessagesBySession(sessionId);
    }

}
