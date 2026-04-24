package edu.ai.lms.core.chat.app.port.in.usecase;

import edu.ai.lms.core.chat.app.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface GetChatHistoryUseCase {
    Page<ChatMessage> getChatHistory(UUID userId1, UUID userId2, Pageable pageable);
}
