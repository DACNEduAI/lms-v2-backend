package edu.ai.lms.database;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Table("courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseJdbcTable {

    @Id
    private UUID id;

    private String courseName;
    private String description;
    private String courseCode;

    @Builder.Default
    private Boolean active = true;

    private String coverImage;

    private LocalDate startDate;
    private LocalDate endDate;

    @MappedCollection(idColumn = "course_id")
    @Builder.Default
    private Set<ChapterJdbcTable> chapters = new HashSet<>();

    @MappedCollection(idColumn = "course_id")
    @Builder.Default
    private Set<CourseTeacherJdbcTable> courseTeachers = new HashSet<>();

    @MappedCollection(idColumn = "course_id")
    @Builder.Default
    private Set<CourseStudentJdbcTable> courseStudents = new HashSet<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
