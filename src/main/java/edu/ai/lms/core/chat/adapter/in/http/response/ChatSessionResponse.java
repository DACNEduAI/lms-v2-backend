package edu.ai.lms.core.chat.adapter.in.http.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dữ liệu trả về cho Client chứa thông tin tóm tắt của một phiên chat.
 * Được sử dụng để hiển thị danh sách các đoạn chat trên Sidebar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionResponse {
    private UUID id;
    private String title;
    private boolean isPinned;
    private LocalDateTime updatedAt;
}
