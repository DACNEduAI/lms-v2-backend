package edu.ai.lms.core.course.adapter.in.mapper;

import edu.ai.lms.core.course.adapter.in.request.ModifyCourseRequest;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.in.command.ModifyCourseCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ModifyCourseCommandMapper {

    public ModifyCourseCommand toCommand(ModifyCourseRequest request) {
        List<LearningOutcome> learningOutcomes = request.getLearningOutcomes().stream()
                .map(learningOutcomeRequest -> LearningOutcome.builder()
                        .code(learningOutcomeRequest.getCode())
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

        return ModifyCourseCommand.builder()
                .courseId(request.getCourseId())
                .course(course)
                .build();
    }
}
