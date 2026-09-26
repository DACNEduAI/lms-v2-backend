package edu.ai.lms.core.course.app.service.ccct;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.CcctProfileStatus;
import edu.ai.lms.core.course.app.exception.CcctConflictException;
import edu.ai.lms.core.course.app.port.in.command.PublishCcctProfileCommand;
import edu.ai.lms.core.course.app.port.in.usecase.PublishCcctProfileUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import edu.ai.lms.core.course.app.port.out.SaveCcctProfilePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublishCcctProfileService implements PublishCcctProfileUseCase {
    private final CcctAccess access;
    private final CcctValidator validator;
    private final CcctProfiles profiles;
    private final SaveCcctProfilePort savePort;
    private final LoadCcctProfilePort loadPort;
    private final CcctActivity activity;
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public CcctProfile publish(PublishCcctProfileCommand command) {
        var context = access.requireCourseTeacher(command.courseId());
        CcctProfile draft = profiles.require(command.courseId(), command.profileId());
        if (draft.getStatus() != CcctProfileStatus.DRAFT) {
            throw new CcctConflictException("Only a DRAFT profile can be published");
        }
        if (draft.getVersion() != command.version()) {
            throw new CcctConflictException(
                    "CCCT draft changed before publish; reload and try again");
        }
        validator.validatePublish(context.course(), draft);

        // A course owns one logical profile.  Published revisions are snapshots,
        // never archived/replaced profile rows.
        LocalDateTime publishedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        if (!savePort.publishDraft(
                command.courseId(),
                command.profileId(),
                command.version(),
                publishedAt)) {
            throw new CcctConflictException(
                    "CCCT draft changed before publish; reload and try again");
        }
        CcctProfile published = loadPort.findByCourseIdAndId(command.courseId(), command.profileId())
                .orElseThrow(() -> new IllegalStateException(
                        "CCCT profile disappeared after publish"));
        snapshot(published, publishedAt);
        activity.publish(context.actor().id(), "PUBLISH", published);
        return published;
    }

    private void snapshot(CcctProfile profile, LocalDateTime publishedAt) {
        jdbc.update("""
                INSERT INTO edu.ccct_profile_revisions
                    (profile_id,published_at,name,template_title,markdown_content)
                VALUES (?,?,?,?,?)
                """, profile.getId(), publishedAt, profile.getName(), profile.getTemplateTitle(), profile.getMarkdownContent());
        for (var chapter : profile.getChapters()) {
            jdbc.update("""
                    INSERT INTO edu.ccct_revision_chapters
                      (profile_id,published_at,id,course_chapter_id,title,order_index)
                    VALUES (?,?,?,?,?,?)
                    """, profile.getId(), publishedAt, chapter.getId(), chapter.getCourseChapterId(), chapter.getTitle(), chapter.getOrderIndex());
            for (var topic : chapter.getTopics()) {
                jdbc.update("""
                        INSERT INTO edu.ccct_revision_topics
                          (profile_id,published_at,id,chapter_id,group_code,title,order_index)
                        VALUES (?,?,?,?,?,?,?)
                        """, profile.getId(), publishedAt, topic.getId(), chapter.getId(), topic.getGroupCode(), topic.getTitle(), topic.getOrderIndex());
                for (var item : topic.getItems()) {
                    jdbc.update("""
                            INSERT INTO edu.ccct_revision_items
                              (profile_id,published_at,id,topic_id,content_code,content,order_index)
                            VALUES (?,?,?,?,?,?,?)
                            """, profile.getId(), publishedAt, item.getId(), topic.getId(), item.getContentCode(), item.getContent(), item.getOrderIndex());
                }
            }
        }
    }
}
