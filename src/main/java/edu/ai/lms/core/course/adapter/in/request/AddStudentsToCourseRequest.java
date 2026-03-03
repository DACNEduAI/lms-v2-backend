package edu.ai.lms.core.course.adapter.in.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddStudentsToCourseRequest {

    @NotEmpty(message = "Student emails list must not be empty")
    private List<String> studentEmails;
}
