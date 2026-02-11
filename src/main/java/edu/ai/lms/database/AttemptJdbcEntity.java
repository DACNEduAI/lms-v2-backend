package edu.ai.lms.database;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptJdbcEntity {

    public enum AttemptStatus {
        STARTED,
        SUBMITTED,
        GRADED
    }

    @Id
    private UUID id;

    @Column("student_id")
    private UUID studentId;

    @Column("quiz_id")
    private UUID quizId;

    private Double score;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AttemptStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
