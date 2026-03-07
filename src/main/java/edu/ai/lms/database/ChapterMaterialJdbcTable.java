package edu.ai.lms.database;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

/**
 * Junction table for Chapter-Material many-to-many relationship.
 * The chapter_number column is managed automatically by @MappedCollection(idColumn = "chapter_number")
 * in ChapterJdbcTable. The course_id is needed because chapter_materials.course_id is NOT NULL
 * (part of the FK constraint back to the composite PK of chapters).
 */
@Table("chapter_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterMaterialJdbcTable {

    @Column("material_id")
    private UUID materialId;

    @Column("course_id")
    private UUID courseId;
}
