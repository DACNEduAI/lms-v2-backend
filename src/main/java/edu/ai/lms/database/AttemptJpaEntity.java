package edu.ai.lms.database;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private StudentJpaEntity student;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private QuizJpaEntity quiz;

    private Double score;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    enum AttemptStatus {
        STARTED,
        SUBMITTED,
        GRADED
    }

    @Enumerated(EnumType.STRING)
    private AttemptStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
