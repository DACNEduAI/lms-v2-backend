package edu.ai.lms.core.material.adapter.in.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyMaterialResponse {
    private UUID id;
    private String title;
    private String type;
    private String fileKey;
    private Boolean isDownloadable;
    private String status;
    private String presignedUploadUrl;
    private UUID courseId;
    private LocalDateTime updatedAt;
}
