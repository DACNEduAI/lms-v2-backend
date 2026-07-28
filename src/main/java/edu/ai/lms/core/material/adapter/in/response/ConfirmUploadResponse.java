package edu.ai.lms.core.material.adapter.in.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmUploadResponse {
    private UUID materialId;
    private UUID courseId;
    private String fileKey;
    private String status;
    private String ingestionStatus;
    private boolean uploaded;
    private String message;
}
