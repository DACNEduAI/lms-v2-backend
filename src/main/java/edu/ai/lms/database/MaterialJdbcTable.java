package edu.ai.lms.database;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialJdbcTable {

    public enum MaterialType {
        PDF,
        VIDEO,
    }

    public enum MaterialStatus {
        PENDING,
        UPLOADED,
        FAILED
    }

    @Id
    private UUID id;

    private String title;
    private MaterialType type;
    private String fileKey;
    private boolean isDownloadable;
    private MaterialStatus status;

    // Direct reference to the owning course
    @Column("course_id")
    private UUID courseId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
