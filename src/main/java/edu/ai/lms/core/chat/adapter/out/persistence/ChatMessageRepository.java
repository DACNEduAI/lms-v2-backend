package edu.ai.lms.core.chat.adapter.out.persistence;

import edu.ai.lms.core.chat.app.domain.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface ChatMessageRepository {

    void insert(ChatMessage message);

    List<ChatMessage> findChatHistory(
            @Param("userId1") UUID userId1,
            @Param("userId2") UUID userId2,
            @Param("offset") long offset,
            @Param("limit") int limit);

    long countChatHistory(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    Optional<ChatMessage> findById(@Param("id") UUID id);

    default ChatMessage save(ChatMessage message) {
        if (message.getId() == null) {
            message.setId(UUID.randomUUID());
            insert(message);
        } else {
            // Usually chat messages are immutable once sent, but add update if needed
            insert(message); // Or update logic
        }
        return findById(message.getId()).orElseThrow();
    }
}
