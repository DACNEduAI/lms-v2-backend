package edu.ai.lms.core.chat.adapter.in.http.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatRequest {
    @NotBlank(message = "Chat message must not be blank")
    @Size(max = 4000, message = "Chat message must be at most 4000 characters")
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
