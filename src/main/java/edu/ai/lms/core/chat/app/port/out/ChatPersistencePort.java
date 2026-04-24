package edu.ai.lms.core.chat.app.port.out;

import edu.ai.lms.core.chat.app.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface ChatPersistencePort {
    ChatMessage save(ChatMessage message);
    Page<ChatMessage> findChatHistory(UUID userId1, UUID userId2, Pageable pageable);
}
