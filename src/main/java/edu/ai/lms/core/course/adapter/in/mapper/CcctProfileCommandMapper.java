package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.CcctChapterRequest;
import edu.ai.lms.core.course.adapter.in.request.CcctItemRequest;
import edu.ai.lms.core.course.adapter.in.request.CcctTopicRequest;
import edu.ai.lms.core.course.adapter.in.request.CreateCcctProfileRequest;
import edu.ai.lms.core.course.adapter.in.request.PublishCcctProfileRequest;
import edu.ai.lms.core.course.adapter.in.request.UpdateCcctProfileRequest;
import edu.ai.lms.core.course.app.port.in.command.CcctChapterValue;
import edu.ai.lms.core.course.app.port.in.command.CcctItemValue;
import edu.ai.lms.core.course.app.port.in.command.CcctProfileContent;
import edu.ai.lms.core.course.app.port.in.command.CcctTopicValue;
import edu.ai.lms.core.course.app.port.in.command.CreateCcctProfileCommand;
import edu.ai.lms.core.course.app.port.in.command.PublishCcctProfileCommand;
import edu.ai.lms.core.course.app.port.in.command.UpdateCcctProfileCommand;

@Component
public class CcctProfileCommandMapper {

    public CreateCcctProfileCommand toCreateCommand(
            UUID courseId,
            CreateCcctProfileRequest request) {
        return new CreateCcctProfileCommand(
                courseId,
                content(
                        request.name(),
                        request.lastMode(),
                        request.formatVersion() == null ? 2 : request.formatVersion(),
                        Boolean.TRUE.equals(request.structuredCurrent()),
                        request.markdown(),
                        request.name(),
                        request.template().chapters()));
    }

    public UpdateCcctProfileCommand toUpdateCommand(
            UUID courseId,
            UUID profileId,
            UpdateCcctProfileRequest request) {
        return new UpdateCcctProfileCommand(
                courseId,
                profileId,
                request.version(),
                content(
                        request.name(),
                        request.lastMode(),
                        request.formatVersion() == null ? 2 : request.formatVersion(),
                        Boolean.TRUE.equals(request.structuredCurrent()),
                        request.markdown(),
                        request.name(),
                        request.template().chapters()),
                request.impactToken());
    }

    public PublishCcctProfileCommand toPublishCommand(
            UUID courseId,
            UUID profileId,
            PublishCcctProfileRequest request) {
        return new PublishCcctProfileCommand(courseId, profileId, request.version());
    }

    private CcctProfileContent content(
            String name,
            edu.ai.lms.core.course.app.domain.CcctMode lastMode,
            int formatVersion,
            boolean structuredCurrent,
            String markdown,
            String title,
            java.util.List<CcctChapterRequest> chapters) {
        return new CcctProfileContent(
                name,
                lastMode,
                formatVersion,
                structuredCurrent,
                markdown,
                title,
                chapters.stream().map(this::chapter).toList());
    }

    private CcctChapterValue chapter(CcctChapterRequest value) {
        return new CcctChapterValue(
                value.id(),
                value.courseChapterId(),
                value.title(),
                value.topics().stream().map(this::topic).toList());
    }

    private CcctTopicValue topic(CcctTopicRequest value) {
        return new CcctTopicValue(
                value.id(),
                value.groupCode(),
                value.title(),
                value.items().stream().map(this::item).toList());
    }

    private CcctItemValue item(CcctItemRequest value) {
        return new CcctItemValue(
                value.id(),
                value.contentCode(),
                value.content());
    }
}
