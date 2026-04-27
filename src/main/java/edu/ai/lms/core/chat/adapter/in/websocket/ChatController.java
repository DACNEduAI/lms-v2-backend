package edu.ai.lms.core.chat.adapter.in.websocket;

import edu.ai.lms.core.chat.app.domain.ChatMessage;
import edu.ai.lms.core.chat.app.port.in.usecase.SendChatMessageUseCase;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SendChatMessageUseCase sendChatMessageUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        if (principal == null) {
            return; // Authentication required
        }

        UUID senderId = UUID.fromString(principal.getName());
        UUID recipientId = request.getRecipientId();

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
