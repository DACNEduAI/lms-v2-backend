package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.response.CcctChapterResponse;
import edu.ai.lms.core.course.adapter.in.response.CcctItemResponse;
import edu.ai.lms.core.course.adapter.in.response.CcctProfileListResponse;
import edu.ai.lms.core.course.adapter.in.response.CcctProfileResponse;
import edu.ai.lms.core.course.adapter.in.response.CcctProfileSummaryResponse;
import edu.ai.lms.core.course.adapter.in.response.CcctTemplateResponse;
import edu.ai.lms.core.course.adapter.in.response.CcctTopicResponse;
import edu.ai.lms.core.course.app.domain.CcctProfile;

@Component
public class CcctProfileResponseMapper {

    public CcctProfileResponse toDetail(CcctProfile profile) {
        return new CcctProfileResponse(
                profile.getId(),
                profile.getCourseId(),
                profile.getName(),
                profile.getStatus(),
                profile.getVersion(),
                profile.getFormatVersion(),
                profile.isStructuredCurrent(),
                profile.getLastMode(),
                profile.getMarkdownContent(),
                new CcctTemplateResponse(
                        profile.getTemplateTitle(),
                        profile.getChapters().stream().map(chapter -> new CcctChapterResponse(
                                chapter.getId(),
                                chapter.getCourseChapterId(),
                                chapter.getTitle(),
                                chapter.getTopics().stream().map(topic -> new CcctTopicResponse(
                                        topic.getId(),
                                        topic.getTitle(),
                                        topic.getItems().stream().map(item -> new CcctItemResponse(
                                                item.getId(),
                                                item.getContent())).toList())).toList())).toList()),
                profile.getCreatedBy(),
                profile.getPublishedAt(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    public CcctProfileListResponse toList(UUID courseId, List<CcctProfile> profiles) {
        List<CcctProfileSummaryResponse> summaries = profiles.stream()
                .map(profile -> new CcctProfileSummaryResponse(
                        profile.getId(),
                        profile.getName(),
                        profile.getStatus(),
                        profile.getVersion(),
                        profile.getFormatVersion(),
                        profile.getLastMode(),
                        profile.getCreatedAt(),
                        profile.getUpdatedAt()))
                .toList();
        return new CcctProfileListResponse(courseId, summaries.size(), summaries);
    }
}
