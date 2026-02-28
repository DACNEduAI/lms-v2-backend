package edu.ai.lms.database;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

/**
 * Junction table for Course-Material many-to-many relationship.
 */
@Table("course_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseMaterialJdbcTable {

    @Column("course_id")
    private UUID courseId;

    @Column("material_id")
    private UUID materialId;
}
