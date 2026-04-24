package edu.ai.lms.core.chat.adapter.out.persistence;

import edu.ai.lms.core.chat.app.domain.ChatMessage;
import edu.ai.lms.core.chat.app.port.out.ChatPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatMessagePersistenceAdapter implements ChatPersistencePort {

    private final ChatMessageRepository repository;

    @Override
    public ChatMessage save(ChatMessage message) {
        ChatMessageJdbcEntity entity = ChatMessageJdbcEntity.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .recipientId(message.getRecipientId())
                .content(message.getContent())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .build();
        
        ChatMessageJdbcEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Page<ChatMessage> findChatHistory(UUID userId1, UUID userId2, Pageable pageable) {
        return repository.findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByCreatedAtDesc(
                userId1, userId2, 
                userId2, userId1, 
                pageable).map(this::toDomain);
    }

    private ChatMessage toDomain(ChatMessageJdbcEntity entity) {
        return ChatMessage.builder()
                .id(entity.getId())
                .senderId(entity.getSenderId())
                .recipientId(entity.getRecipientId())
                .content(entity.getContent())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
