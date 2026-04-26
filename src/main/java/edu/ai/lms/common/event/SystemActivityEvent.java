package edu.ai.lms.common.event;

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
public class SystemActivityEvent {
    private UUID userId;        // Ai làm? (Ví dụ: ID của Thầy giáo)
    private String actionType;  // Làm gì? (Ví dụ: "CREATE")
    private String entityType;  // Tác động lên cái gì? (Ví dụ: "QUIZ")
    private UUID entityId;      // ID của cái đó (Ví dụ: ID của bài Quiz)
    private String entityName;  // Tên của cái đó (Ví dụ: "Bài kiểm tra giữa kỳ")
    private String description; // Mô tả chi tiết (Ví dụ: "Giảng viên vừa tạo bài kiểm tra")
    private LocalDateTime timestamp;
}
