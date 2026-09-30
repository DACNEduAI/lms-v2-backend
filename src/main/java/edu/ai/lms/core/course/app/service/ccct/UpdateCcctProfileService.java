package edu.ai.lms.core.course.app.service.ccct;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.exception.CcctConflictException;
import edu.ai.lms.core.course.app.port.in.command.CcctProfileContent;
import edu.ai.lms.core.course.app.port.in.command.UpdateCcctProfileCommand;
import edu.ai.lms.core.course.app.port.in.usecase.UpdateCcctProfileUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import edu.ai.lms.core.course.app.port.out.SaveCcctProfilePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateCcctProfileService implements UpdateCcctProfileUseCase {
    private final CcctAccess access;
    private final CcctValidator validator;
    private final CcctProfiles profiles;
    private final CcctProfileFactory factory;
    private final SaveCcctProfilePort savePort;
    private final LoadCcctProfilePort loadPort;
    private final CcctActivity activity;
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public CcctProfile update(UpdateCcctProfileCommand command) {
        var context = access.requireCourseTeacher(command.courseId());
        CcctProfile current = profiles.require(command.courseId(), command.profileId());
        requireEditableVersion(current, command.version());
        validator.validateDraft(context.course(), command.content());
        String name = validator.normalizeName(command.content().name());
        profiles.requireUniqueName(command.courseId(), name, command.profileId());

        Set<UUID> removed = removedItems(current, command.content());
        ChangeImpact impact = impact(command.courseId(), command.profileId(), current.getVersion(), removed);
        if (!impact.rows().isEmpty() && !impact.token().equals(command.impactToken())) {
            throw new CcctConflictException("CCCT deletion affects Matrix drafts; preview and confirm the change impact first");
        }

        CcctProfile replacement = factory.replacement(
                command.courseId(),
                command.profileId(),
                current.getCreatedBy(),
                command.content());
        if (!savePort.replaceDraft(replacement, command.version())) {
            throw new CcctConflictException(
                    "CCCT draft changed before this update; reload and try again");
        }
        if (!impact.rows().isEmpty()) {
            // Do not rely on driver-specific UUID[] binding.  The update is deliberately
            // row-by-row: the number of rows in a confirmed deletion is normally small,
            // and it keeps the cascade portable and explicit inside this transaction.
            Set<UUID> matrixIds = impact.rows().stream().map(RowImpact::matrixId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            for (UUID rowId : impact.rows().stream().map(RowImpact::rowId).toList()) {
                jdbc.update("DELETE FROM edu.exam_matrix_rows WHERE id=?", rowId);
            }
            for (UUID matrixId : matrixIds) {
                jdbc.update("UPDATE edu.exam_matrices SET version=version+1, updated_at=CURRENT_TIMESTAMP "
                        + "WHERE id=? AND lifecycle_status='DRAFT'", matrixId);
            }
        }
        CcctProfile updated = loadPort.findByCourseIdAndId(command.courseId(), command.profileId())
                .orElseThrow(() -> new IllegalStateException(
                        "CCCT profile disappeared after update"));
        activity.publish(context.actor().id(), "UPDATE", updated);
        return updated;
    }

    public ChangeImpact preview(UUID courseId, UUID profileId, CcctProfileContent proposed) {
        access.requireCourseTeacher(courseId);
        CcctProfile current = profiles.require(courseId, profileId);
        requireEditableVersion(current, current.getVersion());
        return impact(courseId, profileId, current.getVersion(), removedItems(current, proposed));
    }

    private ChangeImpact impact(UUID courseId, UUID profileId, long lockVersion, Set<UUID> removed) {
        if (removed.isEmpty()) return new ChangeImpact(token(profileId, lockVersion, removed), java.util.List.of());
        String placeholders = String.join(",", java.util.Collections.nCopies(removed.size(), "?"));
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        parameters.add(courseId);
        parameters.addAll(removed);
        java.util.List<RowImpact> rows = jdbc.query("""
                SELECT r.id,m.id,m.title,m.mode,r.ccct_item_id
                FROM edu.exam_matrix_rows r JOIN edu.exam_matrices m ON m.id=r.matrix_id
                WHERE m.course_id=? AND m.lifecycle_status='DRAFT' AND r.ccct_item_id IN (%s)
                ORDER BY m.title,r.sort_order
                """.formatted(placeholders), (rs, n) -> new RowImpact(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), rs.getString(4), rs.getObject(5, UUID.class)), parameters.toArray());
        return new ChangeImpact(token(profileId, lockVersion, removed), rows);
    }

    private Set<UUID> removedItems(CcctProfile current, CcctProfileContent proposed) {
        Set<UUID> before = current.getChapters().stream().flatMap(c -> c.getItems().stream()).map(i -> i.getId()).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> after = proposed.chapters().stream().flatMap(c -> c.items().stream()).map(i -> i.id()).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        before.removeAll(after); return before;
    }
    private static String token(UUID profileId, long lockVersion, Set<UUID> removed) { try { String value=profileId+":"+lockVersion+":"+removed.stream().sorted().map(UUID::toString).collect(Collectors.joining(",")); return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    public record RowImpact(UUID rowId, UUID matrixId, String matrixTitle, String mode, UUID ccctItemId) { }
    public record ChangeImpact(String token, java.util.List<RowImpact> rows) { }

    private void requireEditableVersion(CcctProfile current, long expectedVersion) {
        if (current.getFormatVersion() != 2) {
            throw new CcctConflictException("Legacy CCCT profiles are read-only");
        }
        if (current.getVersion() != expectedVersion) {
            throw new CcctConflictException(
                    "CCCT draft changed before this update; reload and try again");
        }
    }
}
