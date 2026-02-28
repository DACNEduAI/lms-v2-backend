package edu.ai.lms.core.course.app.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.GetChapterResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.exception.ChapterNotFoundException;
import edu.ai.lms.core.course.app.port.in.usecase.GetChapterUseCase;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetChapterService implements GetChapterUseCase {

    private final LoadChapterPort loadChapterPort;

    @Override
    @Transactional(readOnly = true)
    public GetChapterResponse getChapter(UUID courseId, Long chapterNumber) {
        Chapter chapter = loadChapterPort.findByKeyWithoutMaterials(courseId, chapterNumber)
                .orElseThrow(() -> new ChapterNotFoundException(
                        "Chapter not found with courseId: " + courseId + " and chapterNumber: " + chapterNumber));

        return GetChapterResponse.builder()
                .chapterNumber(chapter.getChapterNumber())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .courseId(chapter.getCourseId())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .build();
    }
}
