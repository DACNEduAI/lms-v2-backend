package edu.ai.lms.core.course.app.service.ccct;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.CcctChapter;
import edu.ai.lms.core.course.app.domain.CcctItem;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.CcctProfileStatus;
import edu.ai.lms.core.course.app.domain.CcctTopic;
import edu.ai.lms.core.course.app.port.in.command.CcctProfileContent;

@Component
class CcctProfileFactory {

    CcctProfile createDraft(UUID courseId, UUID createdBy, CcctProfileContent content) {
        return CcctProfile.builder()
                .courseId(courseId)
                .name(content.name().trim())
                .templateTitle(content.templateTitle().trim())
                .status(CcctProfileStatus.DRAFT)
                .version(0)
                .lastMode(content.lastMode())
                .markdownContent(content.markdown())
                .createdBy(createdBy)
                .chapters(content.chapters().stream().map(chapter -> CcctChapter.builder()
                        .id(chapter.id())
                        .courseChapterId(chapter.courseChapterId())
                        .title(chapter.title().trim())
                        .topics(chapter.topics().stream().map(topic -> CcctTopic.builder()
                                .id(topic.id())
                                .title(topic.title().trim())
                                .items(topic.items().stream().map(item -> CcctItem.builder()
                                        .id(item.id())
                                        .content(item.content().trim())
                                        .coverageStatus(item.status())
                                        .note(item.note() == null ? "" : item.note().trim())
                                        .build()).toList())
                                .build()).toList())
                        .build()).toList())
                .build();
    }

    CcctProfile replacement(
            UUID courseId,
            UUID profileId,
            UUID createdBy,
            CcctProfileContent content) {
        CcctProfile profile = createDraft(courseId, createdBy, content);
        profile.setId(profileId);
        return profile;
    }

    CcctProfile cloneDraft(UUID courseId, UUID createdBy, String name, CcctProfile source) {
        return CcctProfile.builder()
                .courseId(courseId)
                .name(name.trim())
                .templateTitle(source.getTemplateTitle())
                .status(CcctProfileStatus.DRAFT)
                .version(0)
                .lastMode(source.getLastMode())
                .markdownContent(source.getMarkdownContent())
                .createdBy(createdBy)
                .chapters(source.getChapters().stream().map(chapter -> CcctChapter.builder()
                        .courseChapterId(chapter.getCourseChapterId())
                        .title(chapter.getTitle())
                        .topics(chapter.getTopics().stream().map(topic -> CcctTopic.builder()
                                .title(topic.getTitle())
                                .items(topic.getItems().stream().map(item -> CcctItem.builder()
                                        .content(item.getContent())
                                        .coverageStatus(item.getCoverageStatus())
                                        .note(item.getNote())
                                        .build()).toList())
                                .build()).toList())
                        .build()).toList())
                .build();
    }
}
