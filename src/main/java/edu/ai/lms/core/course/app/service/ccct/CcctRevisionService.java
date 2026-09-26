package edu.ai.lms.core.course.app.service.ccct;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.exception.CcctConflictException;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import lombok.RequiredArgsConstructor;

/** Lifecycle operations for the one logical CCCT and its immutable snapshots. */
@Service
@RequiredArgsConstructor
public class CcctRevisionService {
    private final CcctAccess access;
    private final CcctProfiles profiles;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<RevisionView> revisions(UUID courseId, UUID profileId) {
        access.requireCourseTeacher(courseId);
        profiles.require(courseId, profileId);
        return jdbc.query("""
                SELECT published_at,name FROM edu.ccct_profile_revisions
                WHERE profile_id=? ORDER BY published_at DESC
                """, (rs, row) -> new RevisionView(rs.getObject(1, LocalDateTime.class), rs.getString(2)), profileId);
    }

    @Transactional(readOnly = true)
    public RevisionDocument revision(UUID courseId, UUID profileId, LocalDateTime publishedAt) {
        access.requireCourseTeacher(courseId);
        profiles.require(courseId, profileId);
        return jdbc.query("""
                SELECT name,template_title,markdown_content FROM edu.ccct_profile_revisions
                WHERE profile_id=? AND published_at=?
                """, rs -> rs.next() ? new RevisionDocument(publishedAt, rs.getString(1), rs.getString(2), rs.getString(3)) : null,
                profileId, publishedAt);
    }

    @Transactional
    public CcctProfile unpublish(UUID courseId, UUID profileId) {
        access.requireCourseTeacher(courseId);
        var profile = profiles.require(courseId, profileId);
        if (profile.getStatus() != edu.ai.lms.core.course.app.domain.CcctProfileStatus.PUBLISHED) {
            throw new CcctConflictException("Only a PUBLISHED CCCT can be unpublished");
        }
        Integer active = jdbc.queryForObject("""
                SELECT count(*) FROM edu.quiz3_assessments
                WHERE course_id=? AND current_step >= 5
                  AND lifecycle_status NOT IN ('PUBLISHED','ARCHIVED')
                """, Integer.class, courseId);
        if (active != null && active > 0) {
            throw new CcctConflictException("CCCT cannot be unpublished while a confirmed Quiz3 is unfinished");
        }
        jdbc.update("""
                UPDATE edu.ccct_profiles SET status='DRAFT', published_at=NULL,
                    version=version+1, updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND course_id=? AND status='PUBLISHED'
                """, profileId, courseId);
        return profiles.require(courseId, profileId);
    }

    public record RevisionView(LocalDateTime publishedAt, String name) { }
    public record RevisionDocument(LocalDateTime publishedAt, String name, String templateTitle, String markdown) { }
}
