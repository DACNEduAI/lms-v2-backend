package edu.ai.lms.core.chat.app.port.out.persistence;

import edu.ai.lms.core.chat.app.domain.AiChatSession;
import edu.ai.lms.core.chat.app.domain.AiCourseChatMessage;
import edu.ai.lms.core.chat.adapter.in.http.request.ChatSessionDto;

import java.util.List;
import java.util.UUID;

public interface AiChatPersistencePort {

    // Session operations
    void saveSession(AiChatSession session);
    List<ChatSessionDto> getSessionsByUserAndCourse(UUID userId, UUID courseId);
    void updateSessionPin(UUID sessionId, boolean isPinned);
    void updateSessionTitle(UUID sessionId, String title);
    void touchSession(UUID sessionId);
    void deleteSession(UUID sessionId);

    // Message operations
    void saveMessage(AiCourseChatMessage message);
    List<AiCourseChatMessage> getMessagesBySession(UUID sessionId);
    int countMessagesBySession(UUID sessionId);

}
