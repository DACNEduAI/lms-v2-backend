package edu.ai.lms.core.chat.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO chứa thông tin phiên chat trả về cho UI.
 * Đặt tại app.dto vì đây là dữ liệu thuộc tầng Application.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDto {
    private UUID id;
    private String title;
    private boolean isPinned;
    private LocalDateTime updatedAt;
}
