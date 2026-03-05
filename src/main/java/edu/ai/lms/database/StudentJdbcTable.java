package edu.ai.lms.database;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentJdbcTable {

    @Id
    private UUID userId;

    private String studentId;

    @Column("enrollment_year")
    private Long enrollmentYear;
}
