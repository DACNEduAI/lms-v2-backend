package edu.ai.lms.core.course.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class ChapterPersistenceAdapterUT {

    @Mock
    private ChapterRepository chapterRepository;

    @Test
    void shouldRemoveQuizLinksBeforeDeletingChapter() {
        UUID courseId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        ChapterPersistenceAdapter adapter = new ChapterPersistenceAdapter(chapterRepository);

        adapter.deleteByKey(courseId, chapterId);

        InOrder deletionOrder = inOrder(chapterRepository);
        deletionOrder.verify(chapterRepository).deleteQuizChapterLinks(chapterId);
        deletionOrder.verify(chapterRepository).deleteByChapterIdAndCourseId(chapterId, courseId);
    }
}
