package edu.ai.lms.core.chat.adapter.out.persistence;

import edu.ai.lms.core.chat.app.domain.ChatMessage;
import edu.ai.lms.core.chat.app.port.out.ChatPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatMessagePersistenceAdapter implements ChatPersistencePort {

    private final ChatMessageRepository repository;

    @Override
    public ChatMessage save(ChatMessage message) {
        return repository.save(message);
    }

    @Override
    public Page<ChatMessage> findChatHistory(UUID userId1, UUID userId2, Pageable pageable) {
        long offset = pageable.getOffset();
        int limit = pageable.getPageSize();
        
        List<ChatMessage> messages = repository.findChatHistory(userId1, userId2, offset, limit);
        long total = repository.countChatHistory(userId1, userId2);
        
        return new PageImpl<>(messages, pageable, total);
    }
}
