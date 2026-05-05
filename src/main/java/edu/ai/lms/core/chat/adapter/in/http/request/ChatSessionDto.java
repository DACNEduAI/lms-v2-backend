package edu.ai.lms.core.chat.adapter.in.http.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDto {
    private UUID id;
    private String title;
    private boolean isPinned;
    private LocalDateTime updatedAt;
}
