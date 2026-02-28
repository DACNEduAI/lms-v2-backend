package edu.ai.lms.database;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("ai_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AISettingsJdbcTable {

    @Id
    private UUID id;

    private String generationPrompt;
    private String difficultyLevel;
    private boolean focusOnWeakAreas;
    private Integer numberOfQuestions;

    /**
     * Discriminator: "STUDENT" or "TEACHER"
     */
    @Column("setting_type")
    private String settingType;

    // Student-specific fields
    private Boolean alignToClo;
    private Boolean includeAnswerKey;
    private String mode;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
