package edu.ai.lms.core.chat.adapter.in.http.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {
    private String userQuery;
    private List<ChatHistoryEntry> chatHistory;
    private List<UUID> selectedMaterialIds; // Source Selection: null hoặc rỗng = tìm toàn bộ khóa học

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatHistoryEntry {
        private String role;
        private String content;
    }
}
