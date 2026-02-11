package edu.ai.lms.database;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Table("teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherJdbcEntity {

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

    private String employeeId;
    private String phoneNumber;
    private String department;
    private String specialization;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
