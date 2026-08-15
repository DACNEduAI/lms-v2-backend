package edu.ai.lms.core.material.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentIndex {
    public enum Status { READY, FAILED }

    private String bucketName;
    private String fileKey;
    private String indexProfile;
    private String sourceHash;
    private String collectionName;
    private Status status;
    private Integer chunkCount;
    private String errorMessage;
    private LocalDateTime indexedAt;
    private LocalDateTime updatedAt;
}
