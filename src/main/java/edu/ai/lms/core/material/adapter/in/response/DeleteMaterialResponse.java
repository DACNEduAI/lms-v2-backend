package edu.ai.lms.core.material.adapter.in.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteMaterialResponse {
    private UUID courseId;
    private UUID materialId;
    private String message;
    private Boolean success;
}
