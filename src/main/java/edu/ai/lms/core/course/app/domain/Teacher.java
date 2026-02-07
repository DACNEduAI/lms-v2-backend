package edu.ai.lms.core.course.app.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Teacher {

    private String employeeId;

    private String phoneNumber;

    private String department;

    private String specialization;

    @Builder.Default
    private List<Course> courses = new ArrayList<>();
}