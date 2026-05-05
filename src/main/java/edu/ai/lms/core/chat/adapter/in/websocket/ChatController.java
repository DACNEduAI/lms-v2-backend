package edu.ai.lms.core.chat.adapter.in.websocket;

import edu.ai.lms.core.chat.app.domain.ChatMessage;
import edu.ai.lms.core.chat.app.port.in.usecase.SendChatMessageUseCase;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor

/**
 * WebSocket controller for handling real-time chat message between teacher and students.
 * This controller listens for messages sent to the "/chat.send" destination,
 * processes them using the SendChatMessageUseCase, and then pushes the saved
 * message to both the recipient and sender via WebSocket.
 *
 * This controller is not used for AI Chat, which is handled separately by AiChatController.
    */

public class ChatController {

    private final SendChatMessageUseCase sendChatMessageUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        if (principal == null) {
            log.warn("Principal is null in sendMessage - UNAUTHORIZED");
            return; // Authentication required
        }

        UUID senderId = UUID.fromString(principal.getName());
        UUID recipientId = request.getRecipientId();
        log.debug("Processing chat message from {} to {}. Content length: {}", 
            senderId, recipientId, request.getContent() != null ? request.getContent().length() : 0);

        ChatMessage savedMessage = sendChatMessageUseCase.sendMessage(senderId, recipientId, request.getContent(), request.getClientTempId());

        // TODO: Refactor the sending message logic
        /*
         * These "Push to recipient" and "Push back to sender" need to be
         * moved to the "out" package
         */
        // Push to recipient
        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/messages",
                savedMessage);

        // Push back to sender (to confirm sent)
        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/messages",
                savedMessage);
    }

    @Data
    public static class ChatMessageRequest {
        private UUID recipientId;
        private String content;
        private String clientTempId;
    }
}
