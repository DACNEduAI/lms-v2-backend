package edu.ai.lms.database;

//import com.learning.lms_platform.content.material.enums.MaterialType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialJpaEntity {

    enum MaterialType {
        PDF,
        VIDEO,
        LINK
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    @Enumerated(EnumType.STRING)
    private MaterialType type; // Enum: PDF, VIDEO, LINK

    private String sourceUrl;
    private boolean isDownloadable;

    @ManyToOne
    @JoinColumn(name = "chapter_id")
    private ChapterJpaEntity chapter;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
