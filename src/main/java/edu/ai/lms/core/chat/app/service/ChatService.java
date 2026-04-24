package edu.ai.lms.core.chat.app.service;

import edu.ai.lms.core.chat.app.domain.ChatMessage;
import edu.ai.lms.core.chat.app.port.in.usecase.GetChatHistoryUseCase;
import edu.ai.lms.core.chat.app.port.in.usecase.SendChatMessageUseCase;
import edu.ai.lms.core.chat.app.port.out.ChatPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService implements SendChatMessageUseCase, GetChatHistoryUseCase {

    private final ChatPersistencePort chatPersistencePort;

    @Override
    public Page<ChatMessage> getChatHistory(UUID userId1, UUID userId2, Pageable pageable) {
        return chatPersistencePort.findChatHistory(userId1, userId2, pageable);
    }

    @Override
    public ChatMessage sendMessage(UUID senderId, UUID recipientId, String content) {
        ChatMessage message = ChatMessage.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .content(content)
                .status("SENT")
                .createdAt(LocalDateTime.now())
                .build();
        return chatPersistencePort.save(message);
    }
}
