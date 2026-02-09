package edu.ai.lms.database;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentJdbcEntity {

    public enum Roles {
        ADMIN,
        TEACHER,
        STUDENT
    }

    @Id
    private UUID id;

    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String avatarUrl;

    private Roles role;

    @Column("enrollment_year")
    private Long enrollmentYear;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
