package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.port.out.DeleteChapterLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.DeleteChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.SaveChapterLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.SaveChapterPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ChapterPersistenceAdapter implements SaveChapterPort, LoadChapterPort, DeleteChapterPort,
        SaveChapterLearningOutcomePort, DeleteChapterLearningOutcomePort {

    private final ChapterRepository chapterRepository;

    @Override
    public Chapter save(Chapter chapter) {
        UUID courseId = chapter.getCourseId();
        boolean isUpdate = chapter.getChapterId() != null;

        Chapter savedChapter;

        if (isUpdate) {
            chapterRepository.updateByCompositeKey(
                    chapter.getChapterId(),
                    courseId,
                    chapter.getTitle(),
                    chapter.getDescription());

            savedChapter = chapterRepository.findByChapterIdAndCourseId(chapter.getChapterId(), courseId)
                    .orElseThrow(() -> new RuntimeException("Chapter not found after update"));
        } else {
            UUID generatedChapterId = chapterRepository.insertAndReturnChapterId(
                    courseId,
                    chapter.getTitle(),
                    chapter.getDescription());

            savedChapter = chapterRepository.findByChapterIdAndCourseId(generatedChapterId, courseId)
                    .orElseThrow(() -> new RuntimeException("Chapter not found after insert"));
        }

        List<UUID> loIds = chapterRepository.findLearningOutcomeIdsByChapterKey(
            savedChapter.getChapterId(), savedChapter.getCourseId());
        savedChapter.setLearningOutcomeIds(loIds);
        return savedChapter;
    }

    @Override
    public Optional<Chapter> findByKey(UUID courseId, UUID chapterId) {
        return chapterRepository.findByChapterIdAndCourseId(chapterId, courseId)
                .map(entity -> {
                    List<UUID> loIds = chapterRepository.findLearningOutcomeIdsByChapterKey(chapterId, courseId);
                    entity.setLearningOutcomeIds(loIds);
                    return entity;
                });
    }

    @Override
    public List<Chapter> findAllByCourseId(UUID courseId) {
        return chapterRepository.findByCourseId(courseId).stream()
                .map(entity -> {
                    List<UUID> loIds = chapterRepository.findLearningOutcomeIdsByChapterKey(
                            entity.getChapterId(), entity.getCourseId());
                entity.setLearningOutcomeIds(loIds);
                return entity;
                })
                .toList();
    }

    @Override
    public void deleteByKey(UUID courseId, UUID chapterId) {
        chapterRepository.deleteByChapterIdAndCourseId(chapterId, courseId);
    }

    @Override
    public void addLearningOutcomeToChapter(UUID chapterId, UUID courseId, UUID learningOutcomeId) {
        chapterRepository.insertChapterLearningOutcome(chapterId, courseId, learningOutcomeId);
    }

    @Override
    public void removeLearningOutcomeFromChapter(UUID chapterId, UUID courseId, UUID learningOutcomeId) {
        chapterRepository.deleteChapterLearningOutcome(chapterId, courseId, learningOutcomeId);
    }
}
