package edu.ai.lms.core.course.app.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {

    private UUID id;

    private String employeeId;

    private String phoneNumber;

    private String department;

    private String specialization;

    @Builder.Default
    private List<Course> courses = new ArrayList<>();
}