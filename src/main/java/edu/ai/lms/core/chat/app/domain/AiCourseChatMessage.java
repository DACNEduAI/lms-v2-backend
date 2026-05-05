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
public class AiCourseChatMessage {
    private UUID id;
    private UUID sessionId;
    private UUID userId;
    private UUID courseId;
    private String role; // 'user' or 'assistant'
    private String content;
    private LocalDateTime createdAt;
}
