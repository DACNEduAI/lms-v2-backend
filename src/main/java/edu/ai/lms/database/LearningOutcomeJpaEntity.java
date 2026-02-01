package edu.ai.lms.database;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "learning_outcomes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningOutcomeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String code; // e.g., "CLO-1"
    private String description;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private CourseJpaEntity course;

    @ManyToMany(mappedBy = "learningOutcomes")
    private List<QuizJpaEntity> quizzes = new ArrayList<>();

    @ManyToMany(mappedBy = "learningOutcomes")
    private List<QuestionJpaEntity> questions = new ArrayList<>();

    @ManyToMany(mappedBy = "masteredLearningOutcomes")
    private List<StudentJpaEntity> students = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
