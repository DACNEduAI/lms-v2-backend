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
public class AiChatSession {
    private UUID id;
    private UUID userId;
    private UUID courseId;
    private String title;
    private boolean isPinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
