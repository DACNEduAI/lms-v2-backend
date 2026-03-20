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
        boolean isUpdate = chapter.getChapterNumber() != null;

        Chapter savedChapter;

        if (isUpdate) {
            chapterRepository.updateByCompositeKey(
                    chapter.getChapterNumber(),
                    courseId,
                    chapter.getTitle(),
                    chapter.getDescription());

            savedChapter = chapterRepository.findByChapterNumberAndCourseId(chapter.getChapterNumber(), courseId)
                    .orElseThrow(() -> new RuntimeException("Chapter not found after update"));
        } else {
            Long generatedChapterNumber = chapterRepository.insertAndReturnChapterNumber(
                    courseId,
                    chapter.getTitle(),
                    chapter.getDescription());

            savedChapter = chapterRepository.findByChapterNumberAndCourseId(generatedChapterNumber, courseId)
                    .orElseThrow(() -> new RuntimeException("Chapter not found after insert"));
        }

        List<UUID> loIds = chapterRepository.findLearningOutcomeIdsByChapterKey(
            savedChapter.getChapterNumber(), savedChapter.getCourseId());
        savedChapter.setLearningOutcomeIds(loIds);
        return savedChapter;
    }

    @Override
    public Optional<Chapter> findByKey(UUID courseId, Long chapterNumber) {
        return chapterRepository.findByChapterNumberAndCourseId(chapterNumber, courseId)
                .map(entity -> {
                    List<UUID> loIds = chapterRepository.findLearningOutcomeIdsByChapterKey(chapterNumber, courseId);
                    entity.setLearningOutcomeIds(loIds);
                    return entity;
                });
    }

    @Override
    public List<Chapter> findAllByCourseId(UUID courseId) {
        return chapterRepository.findByCourseId(courseId).stream()
                .map(entity -> {
                    List<UUID> loIds = chapterRepository.findLearningOutcomeIdsByChapterKey(
                            entity.getChapterNumber(), entity.getCourseId());
                entity.setLearningOutcomeIds(loIds);
                return entity;
                })
                .toList();
    }

    @Override
    public void deleteByKey(UUID courseId, Long chapterNumber) {
        chapterRepository.deleteByChapterNumberAndCourseId(chapterNumber, courseId);
    }

    @Override
    public void addLearningOutcomeToChapter(Long chapterNumber, UUID courseId, UUID learningOutcomeId) {
        chapterRepository.insertChapterLearningOutcome(chapterNumber, courseId, learningOutcomeId);
    }

    @Override
    public void removeLearningOutcomeFromChapter(Long chapterNumber, UUID courseId, UUID learningOutcomeId) {
        chapterRepository.deleteChapterLearningOutcome(chapterNumber, courseId, learningOutcomeId);
    }
}
