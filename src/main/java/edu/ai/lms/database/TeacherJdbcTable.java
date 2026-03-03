package edu.ai.lms.database;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherJdbcTable {

    @Id
    private UUID userId;

    private String employeeId;
    private String phoneNumber;
    private String department;
    private String specialization;
}
