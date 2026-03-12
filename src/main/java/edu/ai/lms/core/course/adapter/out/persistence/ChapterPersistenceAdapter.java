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
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ChapterPersistenceAdapter implements SaveChapterPort, LoadChapterPort, DeleteChapterPort {

    private final ChapterRepository chapterRepository;
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

        return chapterJdbcMapper.mapToChapter(savedChapter);
    }

    @Override
    public Optional<Chapter> findByKey(UUID courseId, Long chapterNumber) {
        return chapterRepository.findByChapterNumberAndCourseId(chapterNumber, courseId)
                .map(chapterJdbcMapper::mapToChapter);
    }

    @Override
    public List<Chapter> findAllByCourseId(UUID courseId) {
        return chapterRepository.findByCourseId(courseId).stream()
                .map(chapterJdbcMapper::mapToChapter)
                .toList();
    }

    @Override
    public void deleteByKey(UUID courseId, Long chapterNumber) {
        // chapter_materials junction rows are cascade-deleted by the database
        chapterRepository.deleteByChapterNumberAndCourseId(chapterNumber, courseId);
    }
}
