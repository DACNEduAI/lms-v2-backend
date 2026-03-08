package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.out.persistence.mapper.ChapterJdbcMapper;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.port.out.DeleteChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.SaveChapterPort;
import edu.ai.lms.database.ChapterJdbcTable;
import edu.ai.lms.database.MaterialJdbcTable;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ChapterPersistenceAdapter implements SaveChapterPort, LoadChapterPort, DeleteChapterPort {

    private final ChapterRepository chapterRepository;
    private final MaterialRepository materialRepository;
    private final ChapterJdbcMapper chapterJdbcMapper;

    @Override
    public Chapter save(Chapter chapter) {
        UUID courseId = chapter.getCourseId();
        boolean isUpdate = chapter.getChapterNumber() != null;

        ChapterJdbcTable savedChapter;

        if (isUpdate) {
            // UPDATE existing chapter using custom method for composite key
            chapterRepository.updateByCompositeKey(
                    chapter.getChapterNumber(),
                    courseId,
                    chapter.getTitle(),
                    chapter.getDescription());

            // Re-fetch the updated chapter
            savedChapter = chapterRepository.findByChapterNumberAndCourseId(chapter.getChapterNumber(), courseId)
                    .orElseThrow(() -> new RuntimeException("Chapter not found after update"));
        } else {
            // INSERT new chapter using custom method that returns generated chapter_number
            Long generatedChapterNumber = chapterRepository.insertAndReturnChapterNumber(
                    courseId,
                    chapter.getTitle(),
                    chapter.getDescription());

            // Fetch the newly created chapter
            savedChapter = chapterRepository.findByChapterNumberAndCourseId(generatedChapterNumber, courseId)
                    .orElseThrow(() -> new RuntimeException("Chapter not found after insert"));
        }

        // Return complete chapter with materials
        return loadChapter(savedChapter);
    }

    @Override
    public Optional<Chapter> findByKey(UUID courseId, Long chapterNumber) {
        return chapterRepository.findByChapterNumberAndCourseId(chapterNumber, courseId)
                .map(this::loadChapter);
    }

    @Override
    public Optional<Chapter> findByKeyWithoutMaterials(UUID courseId, Long chapterNumber) {
        return chapterRepository.findByChapterNumberAndCourseId(chapterNumber, courseId)
                .map(this::loadChapterWithoutMaterials);
    }

    @Override
    public List<Chapter> findAllByCourseId(UUID courseId) {
        return chapterRepository.findByCourseId(courseId).stream()
                .map(this::loadChapterWithoutMaterials)
                .toList();
    }

    @Override
    public void deleteByKey(UUID courseId, Long chapterNumber) {
        // First delete all materials for this chapter
        materialRepository.deleteByChapterKey(chapterNumber, courseId);
        // Then delete the chapter itself
        chapterRepository.deleteByChapterNumberAndCourseId(chapterNumber, courseId);
    }

    /**
     * Loads a full Chapter aggregate with materials by explicitly fetching related entities.
     */
    private Chapter loadChapter(ChapterJdbcTable chapterEntity) {
        List<MaterialJdbcTable> materials = materialRepository.findByChapterKey(
                chapterEntity.getChapterNumber(), chapterEntity.getCourseId());
        return chapterJdbcMapper.mapToChapter(chapterEntity, materials);
    }

    /**
     * Loads a Chapter without materials - only basic chapter data with courseId.
     */
    private Chapter loadChapterWithoutMaterials(ChapterJdbcTable chapterEntity) {
        return chapterJdbcMapper.mapToChapterWithoutMaterials(chapterEntity);
    }
}
