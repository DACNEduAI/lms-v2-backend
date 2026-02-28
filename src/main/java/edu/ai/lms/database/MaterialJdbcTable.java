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
        LINK
    }

    @Id
    private UUID id;

    private String title;
    private MaterialType type;
    private String sourceUrl;
    private boolean isDownloadable;

    // Reference to chapter composite key (chapter_number, course_id)
    @Column("chapter_number")
    private Long chapterNumber;

    @Column("chapter_course_id")
    private UUID chapterCourseId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
