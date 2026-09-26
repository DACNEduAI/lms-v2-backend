package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CcctChapter;
import edu.ai.lms.core.course.app.domain.CcctItem;
import edu.ai.lms.core.course.app.domain.CcctMode;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.CcctProfileStatus;
import edu.ai.lms.core.course.app.domain.CcctTopic;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import edu.ai.lms.core.course.app.port.out.SaveCcctProfilePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CcctProfilePersistenceAdapter
        implements SaveCcctProfilePort, LoadCcctProfilePort {

    private final CcctProfileRepository repository;

    @Override
    @Transactional
    public CcctProfile create(CcctProfile profile) {
        prepareNewProfile(profile);
        repository.insertProfile(profile);
        insertTree(profile);
        return loadRequired(profile.getCourseId(), profile.getId());
    }

    @Override
    @Transactional
    public boolean replaceDraft(CcctProfile profile, long expectedVersion) {
        prepareTree(profile);
        if (repository.updateDraft(profile, expectedVersion) == 0) {
            return false;
        }
        syncTree(profile);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CcctProfile> findByCourseIdAndId(UUID courseId, UUID profileId) {
        return repository.findByCourseIdAndId(courseId, profileId).map(this::loadTree);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CcctProfile> findPublishedByCourseId(UUID courseId) {
        return repository.findPublishedByCourseId(courseId).map(this::loadTree);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CcctProfile> findAllByCourseId(UUID courseId) {
        return repository.findAllByCourseId(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCourseIdAndNameExcludingId(
            UUID courseId,
            String name,
            UUID excludedProfileId) {
        return repository.existsByCourseIdAndNameExcludingId(courseId, name, excludedProfileId);
    }

    @Override
    public boolean publishDraft(
            UUID courseId,
            UUID profileId,
            long expectedVersion,
            java.time.LocalDateTime publishedAt) {
        return repository.publishDraft(courseId, profileId, expectedVersion, publishedAt) == 1;
    }

    private void prepareNewProfile(CcctProfile profile) {
        profile.setId(profile.getId() == null ? UUID.randomUUID() : profile.getId());
        profile.setStatus(profile.getStatus() == null ? CcctProfileStatus.DRAFT : profile.getStatus());
        profile.setLastMode(profile.getLastMode() == null ? CcctMode.STRUCTURED : profile.getLastMode());
        profile.setFormatVersion(profile.getFormatVersion() == 0 ? 2 : profile.getFormatVersion());
        profile.setMarkdownContent(profile.getMarkdownContent() == null ? "" : profile.getMarkdownContent());
        profile.setVersion(0);
        prepareTree(profile);
    }

    private void prepareTree(CcctProfile profile) {
        List<CcctChapter> chapters = profile.getChapters() == null
                ? new ArrayList<>()
                : profile.getChapters();
        profile.setChapters(chapters);
        for (int chapterIndex = 0; chapterIndex < chapters.size(); chapterIndex++) {
            CcctChapter chapter = chapters.get(chapterIndex);
            chapter.setId(chapter.getId() == null ? UUID.randomUUID() : chapter.getId());
            chapter.setProfileId(profile.getId());
            chapter.setOrderIndex(chapterIndex);
            List<CcctTopic> topics = chapter.getTopics() == null
                    ? new ArrayList<>()
                    : chapter.getTopics();
            chapter.setTopics(topics);
            for (int topicIndex = 0; topicIndex < topics.size(); topicIndex++) {
                CcctTopic topic = topics.get(topicIndex);
                topic.setId(topic.getId() == null ? UUID.randomUUID() : topic.getId());
                topic.setProfileId(profile.getId());
                topic.setChapterId(chapter.getId());
                topic.setOrderIndex(topicIndex);
                List<CcctItem> items = topic.getItems() == null
                        ? new ArrayList<>()
                        : topic.getItems();
                topic.setItems(items);
                for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                    CcctItem item = items.get(itemIndex);
                    item.setId(item.getId() == null ? UUID.randomUUID() : item.getId());
                    item.setTopicId(topic.getId());
                    item.setOrderIndex(itemIndex);
                }
            }
        }
    }

    private void insertTree(CcctProfile profile) {
        List<CcctChapter> chapters = profile.getChapters();
        if (chapters.isEmpty()) {
            return;
        }
        repository.insertChapters(chapters);
        List<CcctTopic> topics = chapters.stream()
                .flatMap(chapter -> chapter.getTopics().stream())
                .toList();
        if (topics.isEmpty()) {
            return;
        }
        repository.insertTopics(topics);
        List<CcctItem> items = topics.stream()
                .flatMap(topic -> topic.getItems().stream())
                .toList();
        if (!items.isEmpty()) {
            repository.insertItems(items);
        }
    }

    /**
     * Keep authored logical IDs stable.  A draft update can edit or move an
     * item without rebuilding the complete tree, which is essential because
     * Matrix rows reference those item identities.
     */
    private void syncTree(CcctProfile profile) {
        List<CcctChapter> chapters = profile.getChapters();
        List<CcctTopic> topics = chapters.stream().flatMap(chapter -> chapter.getTopics().stream()).toList();
        List<CcctItem> items = topics.stream().flatMap(topic -> topic.getItems().stream()).toList();
        if (!chapters.isEmpty()) repository.upsertChapters(chapters);
        // Delete absent leaves/groups before inserting updated positions. This
        // releases their (parent, order) unique slots without touching any
        // logical identity that remains in the proposed draft.
        repository.deleteItemsNotIn(profile.getId(), items.stream().map(CcctItem::getId).toList());
        repository.deleteTopicsNotIn(profile.getId(), topics.stream().map(CcctTopic::getId).toList());
        if (!topics.isEmpty()) repository.upsertTopics(topics);
        if (!items.isEmpty()) repository.upsertItems(items);
        repository.deleteChaptersNotIn(profile.getId(), chapters.stream().map(CcctChapter::getId).toList());
    }

    private CcctProfile loadTree(CcctProfile profile) {
        List<CcctChapter> chapters = repository.findChaptersByProfileId(profile.getId());
        profile.setChapters(new ArrayList<>(chapters));
        if (chapters.isEmpty()) {
            return profile;
        }

        Map<UUID, CcctChapter> chaptersById = chapters.stream()
                .collect(Collectors.toMap(
                        CcctChapter::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        List<CcctTopic> topics = repository.findTopicsByChapterIds(chaptersById.keySet());
        topics.forEach(topic -> {
            CcctChapter chapter = chaptersById.get(topic.getChapterId());
            if (chapter == null) {
                throw new IllegalStateException("CCCT topic has no loaded chapter: " + topic.getId());
            }
            chapter.getTopics().add(topic);
        });
        if (topics.isEmpty()) {
            return profile;
        }

        Map<UUID, CcctTopic> topicsById = topics.stream()
                .collect(Collectors.toMap(
                        CcctTopic::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        repository.findItemsByTopicIds(topicsById.keySet()).forEach(item -> {
            CcctTopic topic = topicsById.get(item.getTopicId());
            if (topic == null) {
                throw new IllegalStateException("CCCT item has no loaded topic: " + item.getId());
            }
            topic.getItems().add(item);
        });
        return profile;
    }

    private CcctProfile loadRequired(UUID courseId, UUID profileId) {
        return findByCourseIdAndId(courseId, profileId)
                .orElseThrow(() -> new IllegalStateException("CCCT profile disappeared: " + profileId));
    }
}
