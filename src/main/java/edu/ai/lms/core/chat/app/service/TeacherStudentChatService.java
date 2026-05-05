package edu.ai.lms.core.chat.app.service;

import edu.ai.lms.core.chat.app.domain.ChatMessage;
import edu.ai.lms.core.chat.app.port.in.usecase.GetChatHistoryUseCase;
import edu.ai.lms.core.chat.app.port.in.usecase.SendChatMessageUseCase;
import edu.ai.lms.core.chat.app.port.out.ChatPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherStudentChatService implements SendChatMessageUseCase, GetChatHistoryUseCase {

    private final ChatPersistencePort chatPersistencePort;

    @Override
    public Page<ChatMessage> getChatHistory(UUID userId1, UUID userId2, Pageable pageable) {
        return chatPersistencePort.findChatHistory(userId1, userId2, pageable);
    }

    @Override
    public ChatMessage sendMessage(UUID senderId, UUID recipientId, String content, String clientTempId) {
        ChatMessage message = ChatMessage.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .content(content)
                .status("SENT")
                .clientTempId(clientTempId)
                .createdAt(LocalDateTime.now())
                .build();
        ChatMessage saved = chatPersistencePort.save(message);
        log.debug("Chat message saved successfully. ID: {}, clientTempId: {}", saved.getId(), saved.getClientTempId());
        return saved;
    }
}
