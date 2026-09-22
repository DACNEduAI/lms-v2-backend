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

    @Test
    void shouldMaterializeUnselectedMatrixMappingsAfterAddingOutcomeToChapter() {
        UUID courseId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID outcomeId = UUID.randomUUID();
        ChapterPersistenceAdapter adapter = new ChapterPersistenceAdapter(chapterRepository);

        adapter.addLearningOutcomeToChapter(chapterId, courseId, outcomeId);

        InOrder insertionOrder = inOrder(chapterRepository);
        insertionOrder.verify(chapterRepository)
                .insertChapterLearningOutcome(chapterId, courseId, outcomeId);
    }

    @Test
    void shouldDeleteMatrixMappingsAfterRemovingOutcomeFromChapter() {
        UUID courseId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID outcomeId = UUID.randomUUID();
        ChapterPersistenceAdapter adapter = new ChapterPersistenceAdapter(chapterRepository);

        adapter.removeLearningOutcomeFromChapter(chapterId, courseId, outcomeId);

        InOrder deletionOrder = inOrder(chapterRepository);
        deletionOrder.verify(chapterRepository)
                .deleteChapterLearningOutcome(chapterId, courseId, outcomeId);
    }
}
