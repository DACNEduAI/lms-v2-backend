package edu.ai.lms.core.material.adapter.in.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyMaterialRequest {

    private String title;

    private String type; // PDF, VIDEO, LINK

    private Boolean isDownloadable;
}
