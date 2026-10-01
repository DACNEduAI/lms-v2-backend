package edu.ai.lms.core.course.app.service.ccct;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import edu.ai.lms.core.course.app.domain.CcctChapter;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.exception.ChapterCcctSyncConflictException;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import edu.ai.lms.core.course.app.port.out.SaveCcctProfilePort;
import lombok.RequiredArgsConstructor;

/** Keeps the one course-level CCCT tree aligned with course Chapter changes. */
@Service
@RequiredArgsConstructor
public class CcctCourseChapterSynchronizer {
    private final LoadCcctProfilePort profiles;
    private final SaveCcctProfilePort saveProfiles;
    private final JdbcTemplate jdbc;

    public void chapterCreated(UUID courseId, Chapter chapter) {
        CcctProfile profile = currentProfile(courseId);
        if (profile == null) return;
        Set<String> codes = new HashSet<>();
        profile.getChapters().forEach(value -> codes.add(value.getChapterCode()));
        profile.getChapters().add(CcctChapter.builder()
                .courseChapterId(chapter.getChapterId())
                .chapterCode(uniqueCode(chapter.getTitle(), codes))
                .title(chapter.getTitle())
                .items(new ArrayList<>())
                .build());
        save(profile);
    }

    public void chapterRenamed(UUID courseId, UUID chapterId, String title) {
        CcctProfile profile = currentProfile(courseId);
        if (profile == null) return;
        boolean changed = profile.getChapters().stream()
                .filter(value -> chapterId.equals(value.getCourseChapterId()))
                .findFirst()
                .map(value -> {
                    if (title.equals(value.getTitle())) return false;
                    value.setTitle(title);
                    return true;
                }).orElse(false);
        if (changed) save(profile);
    }

    public void ensureChapterDeletable(UUID courseId, UUID chapterId) {
        CcctProfile profile = currentProfile(courseId);
        if (profile == null) return;
        CcctChapter target = profile.getChapters().stream()
                .filter(value -> chapterId.equals(value.getCourseChapterId()))
                .findFirst().orElse(null);
        if (target == null) return;
        List<UUID> itemIds = target.getItems().stream().map(value -> value.getId()).toList();
        if (itemIds.isEmpty()) return;
        String placeholders = String.join(",", java.util.Collections.nCopies(itemIds.size(), "?"));
        List<Object> parameters = new ArrayList<>();
        parameters.add(courseId);
        parameters.addAll(itemIds);
        List<ChapterCcctSyncConflictException.MatrixImpact> impacts = jdbc.query("""
                SELECT m.id, m.title, COUNT(*)
                FROM edu.exam_matrix_rows r JOIN edu.exam_matrices m ON m.id=r.matrix_id
                WHERE m.course_id=? AND m.lifecycle_status='DRAFT' AND r.ccct_item_id IN (%s)
                GROUP BY m.id, m.title ORDER BY m.title, m.id
                """.formatted(placeholders),
                (rs, row) -> new ChapterCcctSyncConflictException.MatrixImpact(
                        rs.getObject(1, UUID.class), rs.getString(2), rs.getInt(3)), parameters.toArray());
        if (!impacts.isEmpty()) throw new ChapterCcctSyncConflictException(impacts);
    }

    public void chapterDeleted(UUID courseId, UUID chapterId) {
        CcctProfile profile = currentProfile(courseId);
        if (profile == null) return;
        boolean removed = profile.getChapters().removeIf(value -> chapterId.equals(value.getCourseChapterId()));
        if (removed) save(profile);
    }

    private CcctProfile currentProfile(UUID courseId) {
        return profiles.findAllByCourseId(courseId).stream().findFirst()
                .flatMap(summary -> profiles.findByCourseIdAndId(courseId, summary.getId()))
                .orElse(null);
    }

    private void save(CcctProfile profile) {
        profile.setMarkdownContent(markdown(profile));
        if (!saveProfiles.replaceDraft(profile, profile.getVersion())) {
            throw new IllegalStateException("CCCT changed concurrently; Chapter change was rolled back");
        }
    }

    private String uniqueCode(String title, Set<String> existing) {
        String normalized = Normalizer.normalize((title == null ? "" : title).replace('Đ', 'D').replace('đ', 'd'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]+", " ").trim();
        StringBuilder value = new StringBuilder();
        for (String word : normalized.split("\\s+")) {
            if (word.isBlank()) continue;
            String lower = word.toLowerCase(Locale.ROOT);
            value.append(value.isEmpty() ? lower : Character.toUpperCase(lower.charAt(0)) + lower.substring(1));
        }
        String base = value.isEmpty() ? "chapter" : value.toString();
        if (Character.isDigit(base.charAt(0))) base = "chapter" + base;
        base = base.substring(0, Math.min(base.length(), 64));
        String candidate = base;
        int suffix = 2;
        while (existing.contains(candidate)) {
            String number = String.valueOf(suffix++);
            candidate = base.substring(0, Math.min(base.length(), 64 - number.length())) + number;
        }
        return candidate;
    }

    private String markdown(CcctProfile profile) {
        List<String> lines = new ArrayList<>();
        lines.add("# " + profile.getTemplateTitle().trim());
        for (CcctChapter chapter : profile.getChapters()) {
            lines.add("");
            lines.add("## " + chapter.getTitle().trim() + " [" + chapter.getChapterCode().trim() + "]");
            chapter.getItems().forEach(item -> lines.add("- " + item.getContent().trim() + " [" + item.getContentCode().trim() + "]"));
        }
        return String.join("\n", lines);
    }
}
