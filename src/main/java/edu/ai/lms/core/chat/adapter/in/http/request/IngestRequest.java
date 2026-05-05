package edu.ai.lms.core.chat.adapter.in.http.request;

import lombok.Builder;
import lombok.Data;

/**
 * Request nhận từ Frontend/Material service để yêu cầu AI nạp tài liệu.
 * Đặt tại adapter.in vì đây là dữ liệu đầu vào (Inbound).
 */
@Data
@Builder
public class IngestRequest {
    private String course_id;
    private String material_id;
    private String file_url;
}
