package edu.ai.lms.core.chat.app.port.in.usecase;

import edu.ai.lms.core.chat.app.domain.ChatMessage;
import java.util.UUID;

public interface SendChatMessageUseCase {
    ChatMessage sendMessage(UUID senderId, UUID recipientId, String content);
}
