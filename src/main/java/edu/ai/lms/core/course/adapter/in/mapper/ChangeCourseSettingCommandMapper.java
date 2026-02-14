package edu.ai.lms.core.course.adapter.in.mapper;

import edu.ai.lms.core.course.adapter.in.request.ChangeCourseSettingRequest;
import edu.ai.lms.core.course.app.port.in.command.ChangeCourseSettingCommand;
import org.springframework.stereotype.Component;

@Component
public class ChangeCourseSettingCommandMapper {

    public ChangeCourseSettingCommand toCommand(ChangeCourseSettingRequest request) {
        return ChangeCourseSettingCommand.builder()
                .courseId(request.getCourseId())
                .courseName(request.getCourseName())
                .description(request.getDescription())
                .active(request.getActive())
                .coverImage(request.getCoverImage())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
    }
}
