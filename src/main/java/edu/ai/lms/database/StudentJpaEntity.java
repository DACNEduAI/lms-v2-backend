package edu.ai.lms.database;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StudentJpaEntity extends UserJpaEntity {
    @Column(nullable = false)
    private Long enrollmentYear;

    @OneToMany(mappedBy = "student")
    private List<AttemptJpaEntity> attempts;

    @OneToMany(mappedBy = "student")
    private List<QuizJpaEntity> quizzes;

    @ManyToMany
    @JoinTable(
        name = "student_learning_outcomes",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "learning_outcome_id")
    )
    private List<LearningOutcomeJpaEntity> masteredLearningOutcomes;
}
