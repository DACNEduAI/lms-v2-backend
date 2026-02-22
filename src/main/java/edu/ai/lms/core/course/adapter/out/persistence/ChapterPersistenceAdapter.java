package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.out.persistence.mapper.ChapterJdbcMapper;
import edu.ai.lms.core.course.adapter.out.persistence.mapper.MaterialJdbcMapper;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.SaveChapterPort;
import edu.ai.lms.database.ChapterJdbcEntity;
import edu.ai.lms.database.MaterialJdbcEntity;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ChapterPersistenceAdapter implements SaveChapterPort, LoadChapterPort {

    private final ChapterRepository chapterRepository;
    private final MaterialRepository materialRepository;
    private final ChapterJdbcMapper chapterJdbcMapper;
    private final MaterialJdbcMapper materialJdbcMapper;

    @Override
    public Chapter save(Chapter chapter) {
        UUID courseId = chapter.getCourseId();

        // Save the chapter entity
        ChapterJdbcEntity chapterEntity = chapterJdbcMapper.mapToChapterJdbcEntity(chapter, courseId);
        ChapterJdbcEntity savedChapter = chapterRepository.save(chapterEntity);

        // Save materials for this chapter
        if (chapter.getMaterials() != null) {
            chapter.getMaterials().forEach(material -> {
                MaterialJdbcEntity materialEntity = materialJdbcMapper.mapToMaterialJdbcEntity(material);
                materialEntity.setChapterId(savedChapter.getId());
                materialRepository.save(materialEntity);
            });
        }

        // Re-load to return complete chapter with generated IDs and timestamps
        return loadChapter(savedChapter);
    }

    @Override
    public Optional<Chapter> findById(UUID chapterId) {
        return chapterRepository.findById(chapterId)
                .map(this::loadChapter);
    }

    @Override
    public Optional<Chapter> findByIdWithoutMaterials(UUID chapterId) {
        return chapterRepository.findById(chapterId)
                .map(this::loadChapterWithoutMaterials);
    }

    /**
     * Loads a full Chapter aggregate with materials by explicitly fetching related entities.
     */
    private Chapter loadChapter(ChapterJdbcEntity chapterEntity) {
        List<MaterialJdbcEntity> materials = materialRepository.findByChapterId(chapterEntity.getId());
        return chapterJdbcMapper.mapToChapter(chapterEntity, materials);
    }

    /**
     * Loads a Chapter without materials - only basic chapter data with courseId.
     */
    private Chapter loadChapterWithoutMaterials(ChapterJdbcEntity chapterEntity) {
        return chapterJdbcMapper.mapToChapterWithoutMaterials(chapterEntity);
    }
}
