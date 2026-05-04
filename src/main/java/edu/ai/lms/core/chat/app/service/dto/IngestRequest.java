package edu.ai.lms.core.chat.app.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IngestRequest {
    private String course_id;
    private String material_id;
    private String file_url;
}
