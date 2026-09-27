package edu.ai.lms.core.course.app.service.ccct;

import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import edu.ai.lms.core.course.app.domain.CcctChapter;
import edu.ai.lms.core.course.app.domain.CcctItem;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.CcctProfileStatus;
import edu.ai.lms.core.course.app.port.in.command.CcctProfileContent;

@Component
@RequiredArgsConstructor
class CcctProfileFactory {
    private final CcctMarkdownSerializer markdownSerializer;

    CcctProfile createDraft(UUID courseId, UUID createdBy, CcctProfileContent content) {
        CcctProfile profile = CcctProfile.builder()
                .courseId(courseId)
                .name(content.name().trim())
                .templateTitle(content.name().trim())
                .status(CcctProfileStatus.DRAFT)
                .version(0)
                .formatVersion(content.formatVersion())
                .structuredCurrent(content.structuredCurrent())
                .lastMode(content.lastMode())
                .markdownContent("")
                .createdBy(createdBy)
                .chapters(content.chapters().stream().map(chapter -> CcctChapter.builder()
                        .id(chapter.id())
                        .courseChapterId(chapter.courseChapterId())
                        .chapterCode(chapter.chapterCode().trim())
                        .title(chapter.title().trim())
                        .items(java.util.stream.IntStream.range(0, chapter.items().size()).mapToObj(itemIndex -> {
                            var item = chapter.items().get(itemIndex);
                            String code = item.contentCode() == null || item.contentCode().isBlank()
                                    ? chapter.chapterCode().trim() + ".item" + (itemIndex + 1)
                                    : item.contentCode().trim();
                            return CcctItem.builder().id(item.id()).contentCode(code).content(item.content().trim()).build();
                        }).toList())
                        .build()).toList())
                .build();
        profile.setMarkdownContent(markdownSerializer.serialize(profile));
        return profile;
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
                .templateTitle(name.trim())
                .status(CcctProfileStatus.DRAFT)
                .version(0)
                .formatVersion(source.getFormatVersion())
                .structuredCurrent(source.isStructuredCurrent())
                .lastMode(source.getLastMode())
                .markdownContent(source.getMarkdownContent())
                .createdBy(createdBy)
                .chapters(source.getChapters().stream().map(chapter -> CcctChapter.builder()
                        .courseChapterId(chapter.getCourseChapterId())
                        .chapterCode(chapter.getChapterCode())
                        .title(chapter.getTitle())
                        .items(chapter.getItems().stream().map(item -> CcctItem.builder()
                                .contentCode(item.getContentCode()).content(item.getContent()).build()).toList())
                        .build()).toList())
                .build();
    }
}
