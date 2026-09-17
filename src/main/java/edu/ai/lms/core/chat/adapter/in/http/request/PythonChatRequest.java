package edu.ai.lms.core.chat.adapter.in.http.request;

import edu.ai.lms.core.chat.adapter.in.http.request.AiChatRequest.ChatHistoryEntry;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PythonChatRequest {
    private String course_id; // RAG server Python yêu cầu snake_case
    private String user_query;
    private List<ChatHistoryEntry> chat_history;
    private List<String> selected_material_ids; // Source Selection: null = tìm toàn bộ
    private CourseMetadata course_metadata;
    private LearnerState learner_state;

    @Data
    @Builder
    public static class CourseMetadata {
        private String id;
        private String code;
        private String name;
        private String description;
        private List<LearningOutcome> learning_outcomes;
        private List<MaterialMetadata> materials;
    }

    @Data
    @Builder
    public static class LearningOutcome {
        private String id;
        private String code;
        private String description;
    }

    @Data
    @Builder
    public static class MaterialMetadata {
        private String id;
        private String title;
        private String original_file_name;
        private String type;
    }

    @Data
    @Builder
    public static class LearnerState {
        private LocalDateTime calculated_at;
        private List<LearnerStateOutcome> outcomes;
    }

    @Data
    @Builder
    public static class LearnerStateOutcome {
        private String learning_outcome_id;
        private String code;
        private String description;
        private Double probability;
        private String status;
    }
}
