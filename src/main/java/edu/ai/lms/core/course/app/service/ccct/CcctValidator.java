package edu.ai.lms.core.course.app.service.ccct;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.CcctCoverageStatus;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CcctPayloadTooLargeException;
import edu.ai.lms.core.course.app.exception.CcctValidationException;
import edu.ai.lms.core.course.app.port.in.command.CcctProfileContent;

@Component
class CcctValidator {
    static final int MAX_CHAPTERS = 100;
    static final int MAX_TOPICS_PER_CHAPTER = 200;
    static final int MAX_ITEMS_PER_TOPIC = 500;
    static final int MAX_PAYLOAD_BYTES = 2 * 1024 * 1024;

    void validateDraft(Course course, CcctProfileContent content) {
        if (content == null) {
            invalid("Profile content is required");
        }
        required(content.name(), "Profile name", 255);
        if (content.lastMode() == null) {
            invalid("lastMode is required");
        }
        if (content.formatVersion() != 2) {
            invalid("Only CCCT format version 2 can be created or updated");
        }
        if (content.markdown() == null) {
            invalid("markdown is required");
        }
        required(content.templateTitle(), "Template title", 255);
        if (content.chapters().size() > MAX_CHAPTERS) {
            invalid("A profile cannot contain more than " + MAX_CHAPTERS + " chapters");
        }

        Set<UUID> nodeIds = new HashSet<>();
        if (content.chapters().size() != course.getChapters().size()) {
            invalid("CCCT must contain every Course Chapter exactly once");
        }
        long payloadBytes = utf8(content.name())
                + utf8(content.markdown())
                + utf8(content.templateTitle());

        for (int chapterIndex = 0; chapterIndex < content.chapters().size(); chapterIndex++) {
            var chapter = content.chapters().get(chapterIndex);
            var courseChapter = course.getChapters().get(chapterIndex);
            uniqueNodeId(nodeIds, chapter.id());
            required(chapter.title(), "Chapter title", 255);
            if (chapter.courseChapterId() == null
                    || !chapter.courseChapterId().equals(courseChapter.getChapterId())) {
                invalid("CCCT chapters must match the Course Chapter order");
            }
            if (!chapter.title().trim().equals(courseChapter.getTitle().trim())) {
                invalid("CCCT chapter titles must match the Course Chapters");
            }
            if (chapter.topics().size() > MAX_TOPICS_PER_CHAPTER) {
                invalid("A chapter cannot contain more than "
                        + MAX_TOPICS_PER_CHAPTER + " topics");
            }
            payloadBytes += utf8(chapter.title()) + utf8(chapter.customContent());
            for (var topic : chapter.topics()) {
                uniqueNodeId(nodeIds, topic.id());
                required(topic.title(), "Topic title", 255);
                if (topic.items().size() > MAX_ITEMS_PER_TOPIC) {
                    invalid("A topic cannot contain more than "
                            + MAX_ITEMS_PER_TOPIC + " items");
                }
                payloadBytes += utf8(topic.title());
                for (var item : topic.items()) {
                    uniqueNodeId(nodeIds, item.id());
                    required(item.content(), "Item content", Integer.MAX_VALUE);
                    if (item.status() == null) {
                        invalid("Item status is required");
                    }
                    if (item.note() != null && item.note().length() > 2_000) {
                        invalid("Item note cannot exceed 2000 characters");
                    }
                    payloadBytes += utf8(item.content()) + utf8(item.note());
                    if (payloadBytes > MAX_PAYLOAD_BYTES) {
                        throw new CcctPayloadTooLargeException(
                                "CCCT payload cannot exceed 2 MB");
                    }
                }
            }
        }
        if (payloadBytes > MAX_PAYLOAD_BYTES) {
            throw new CcctPayloadTooLargeException("CCCT payload cannot exceed 2 MB");
        }
    }

    void validatePublish(Course course, CcctProfile profile) {
        if (profile.getFormatVersion() != 2) {
            invalid("Legacy CCCT profiles are read-only and cannot be published");
        }
        Set<UUID> courseChapterIds = new HashSet<>();
        course.getChapters().forEach(chapter -> courseChapterIds.add(chapter.getChapterId()));
        int examItemCount = 0;
        for (var chapter : profile.getChapters()) {
            for (var topic : chapter.getTopics()) {
                for (var item : topic.getItems()) {
                    if (item.getCoverageStatus() == CcctCoverageStatus.LEARN_AND_EXAM) {
                        examItemCount++;
                        if (chapter.getCourseChapterId() == null
                                || !courseChapterIds.contains(chapter.getCourseChapterId())) {
                            invalid("Every LEARN_AND_EXAM item must belong to a mapped Course Chapter");
                        }
                    }
                }
            }
        }
        if (examItemCount == 0) {
            invalid("A published CCCT must contain at least one LEARN_AND_EXAM item");
        }
    }

    String normalizeName(String name) {
        required(name, "Profile name", 255);
        return name.trim();
    }

    private void uniqueNodeId(Set<UUID> nodeIds, UUID id) {
        if (id != null && !nodeIds.add(id)) {
            invalid("Node IDs must be unique within the request: " + id);
        }
    }

    private void required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            invalid(field + " cannot be blank");
        }
        if (value.trim().length() > maxLength) {
            invalid(field + " cannot exceed " + maxLength + " characters");
        }
    }

    private int utf8(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }

    private void invalid(String message) {
        throw new CcctValidationException(message);
    }
}
