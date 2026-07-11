package edu.ai.lms.core.course.adapter.in.mapper;

import edu.ai.lms.core.course.adapter.in.request.CreateCourseRequest;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.in.command.CreateCourseCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CreateCourseCommandMapper {

    public CreateCourseCommand toCommand(CreateCourseRequest request) {
        List<LearningOutcome> learningOutcomes = request.getLearningOutcomes().stream()
                .map(learningOutcomeRequest -> LearningOutcome.builder()
                        .prefixNumber(learningOutcomeRequest.getPrefixNumber())
                        .sortOrder(learningOutcomeRequest.getPrefixNumber())
                        .description(learningOutcomeRequest.getDescription())
                        .build())
                .collect(Collectors.toList());

        Course course = Course.builder()
                .courseName(request.getCourseName())
                .description(request.getDescription())
                .courseCode(request.getCourseCode())
                .active(request.getActive())
                .coverImage(request.getCoverImage())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .learningOutcomes(learningOutcomes)
                .build();

        return CreateCourseCommand.builder()
                .course(course)
                .build();
    }
}
