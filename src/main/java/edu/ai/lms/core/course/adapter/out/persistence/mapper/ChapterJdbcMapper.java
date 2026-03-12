package edu.ai.lms.core.course.adapter.out.persistence.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.database.ChapterJdbcTable;

@Component
public class ChapterJdbcMapper {

    public ChapterJdbcTable mapToChapterJdbcEntity(Chapter chapter, UUID courseId) {
        return ChapterJdbcTable.builder()
                .chapterNumber(chapter.getChapterNumber())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .courseId(courseId)
                .build();
    }

    public Chapter mapToChapter(ChapterJdbcTable entity) {
        return Chapter.builder()
                .chapterNumber(entity.getChapterNumber())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .courseId(entity.getCourseId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
