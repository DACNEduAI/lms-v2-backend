package edu.ai.lms.core.chat.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private UUID id;
    private UUID senderId;
    private UUID recipientId;
    private String content;
    private String status; // SENT, DELIVERED, READ
    private LocalDateTime createdAt;
}
