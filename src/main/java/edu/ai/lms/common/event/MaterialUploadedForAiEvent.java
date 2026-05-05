package edu.ai.lms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialUploadedForAiEvent {
    private UUID courseId;
    private UUID materialId;
    private String fileUrl;
}
