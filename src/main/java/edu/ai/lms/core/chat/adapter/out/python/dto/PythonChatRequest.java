package edu.ai.lms.core.chat.adapter.out.python.dto;

import edu.ai.lms.core.chat.adapter.in.http.request.AiChatRequest.ChatHistoryEntry;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO phục vụ việc gọi API sang Python RAG Server.
 * Đặt tại adapter.out vì đây là dữ liệu đi ra (Outbound).
 */
@Data
@Builder
public class PythonChatRequest {
    private String course_id; // RAG server Python yêu cầu snake_case
    private String user_query;
    private List<ChatHistoryEntry> chat_history;
    private List<String> selected_material_ids; // Source Selection: null = tìm toàn bộ
}
