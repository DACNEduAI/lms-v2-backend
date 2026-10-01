package edu.ai.lms.core.course.app.service.ccct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import edu.ai.lms.core.course.app.domain.CcctChapter;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.exception.ChapterCcctSyncConflictException;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import edu.ai.lms.core.course.app.port.out.SaveCcctProfilePort;

@ExtendWith(MockitoExtension.class)
class CcctCourseChapterSynchronizerUT {
    @Mock private LoadCcctProfilePort profiles;
    @Mock private SaveCcctProfilePort saveProfiles;
    @Mock private JdbcTemplate jdbc;
    @InjectMocks private CcctCourseChapterSynchronizer synchronizer;

    private UUID courseId;
    private CcctProfile profile;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        profile = CcctProfile.builder().id(UUID.randomUUID()).courseId(courseId)
                .templateTitle("Coverage").version(3).chapters(new ArrayList<>(List.of(
                        CcctChapter.builder().id(UUID.randomUUID()).courseChapterId(UUID.randomUUID())
                                .chapterCode("deQuy").title("Đệ quy").items(new ArrayList<>()).build())))
                .build();
        when(profiles.findAllByCourseId(courseId)).thenReturn(List.of(profile));
        when(profiles.findByCourseIdAndId(courseId, profile.getId())).thenReturn(Optional.of(profile));
        when(saveProfiles.replaceDraft(any(), anyLong())).thenReturn(true);
    }

    @Test
    void createsUniqueNormalizedCodeAndDerivedMarkdown() {
        Chapter chapter = Chapter.builder().chapterId(UUID.randomUUID()).title("Đệ quy").build();

        synchronizer.chapterCreated(courseId, chapter);

        ArgumentCaptor<CcctProfile> saved = ArgumentCaptor.forClass(CcctProfile.class);
        verify(saveProfiles).replaceDraft(saved.capture(), anyLong());
        assertEquals("deQuy2", saved.getValue().getChapters().get(1).getChapterCode());
        assertEquals("# Coverage\n\n## Đệ quy [deQuy]\n\n## Đệ quy [deQuy2]", saved.getValue().getMarkdownContent());
    }

    @Test
    void prefixesGeneratedCodeWhenTheTitleStartsWithANumber() {
        synchronizer.chapterCreated(courseId, Chapter.builder().chapterId(UUID.randomUUID()).title("101 Trees").build());

        assertEquals("chapter101Trees", profile.getChapters().get(1).getChapterCode());
    }

    @Test
    void doesNothingWhenTheCourseHasNoCcct() {
        when(profiles.findAllByCourseId(courseId)).thenReturn(List.of());

        synchronizer.chapterCreated(courseId, Chapter.builder().chapterId(UUID.randomUUID()).title("New chapter").build());

        org.mockito.Mockito.verifyNoInteractions(saveProfiles);
    }

    @Test
    void keepsCodeAndItemsWhenChapterIsRenamed() {
        UUID chapterId = profile.getChapters().get(0).getCourseChapterId();

        synchronizer.chapterRenamed(courseId, chapterId, "Recursion");

        assertEquals("deQuy", profile.getChapters().get(0).getChapterCode());
        assertEquals("Recursion", profile.getChapters().get(0).getTitle());
        verify(saveProfiles).replaceDraft(profile, 3);
    }

    @Test
    void removesOnlyTheDeletedChapterBranch() {
        UUID deletedChapterId = profile.getChapters().get(0).getCourseChapterId();
        profile.getChapters().add(CcctChapter.builder().id(UUID.randomUUID()).courseChapterId(UUID.randomUUID())
                .chapterCode("trees").title("Trees").items(new ArrayList<>()).build());

        synchronizer.chapterDeleted(courseId, deletedChapterId);

        assertEquals(1, profile.getChapters().size());
        assertEquals("trees", profile.getChapters().get(0).getChapterCode());
        verify(saveProfiles).replaceDraft(profile, 3);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void blocksDeletionWhenDraftMatrixUsesTheChapterContent() {
        UUID itemId = UUID.randomUUID();
        profile.getChapters().get(0).setItems(new ArrayList<>(List.of(
                edu.ai.lms.core.course.app.domain.CcctItem.builder().id(itemId).build())));
        var impact = new ChapterCcctSyncConflictException.MatrixImpact(UUID.randomUUID(), "Midterm", 2);
        when(jdbc.query(org.mockito.ArgumentMatchers.anyString(), (RowMapper) any(RowMapper.class),
                org.mockito.ArgumentMatchers.any(Object[].class))).thenReturn(List.of(impact));

        ChapterCcctSyncConflictException error = assertThrows(ChapterCcctSyncConflictException.class,
                () -> synchronizer.ensureChapterDeletable(courseId, profile.getChapters().get(0).getCourseChapterId()));

        assertEquals("Midterm", error.getAffectedMatrices().get(0).title());
        assertEquals(2, error.getAffectedMatrices().get(0).affectedRowCount());
    }
}
