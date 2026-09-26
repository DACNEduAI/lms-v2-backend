package edu.ai.lms.core.course.app.service.ccct;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.jdbc.core.JdbcTemplate;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.CcctProfileStatus;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.app.exception.CcctConflictException;
import edu.ai.lms.core.course.app.port.in.command.PublishCcctProfileCommand;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import edu.ai.lms.core.course.app.port.out.SaveCcctProfilePort;

@ExtendWith(MockitoExtension.class)
class PublishCcctProfileServiceUT {
    @Mock
    private CcctAccess access;
    @Mock
    private CcctValidator validator;
    @Mock
    private CcctProfiles profiles;
    @Mock
    private SaveCcctProfilePort savePort;
    @Mock
    private LoadCcctProfilePort loadPort;
    @Mock
    private CcctActivity activity;
    @Mock
    private JdbcTemplate jdbc;
    @InjectMocks
    private PublishCcctProfileService service;

    @Test
    void rejectsStaleVersionBeforePublishingDraft() {
        UUID courseId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        Course course = Course.builder().id(courseId).build();
        CcctProfile draft = CcctProfile.builder()
                .id(profileId)
                .status(CcctProfileStatus.DRAFT)
                .version(3)
                .build();
        when(access.requireCourseTeacher(courseId)).thenReturn(context(course));
        when(profiles.require(courseId, profileId)).thenReturn(draft);

        assertThatThrownBy(() -> service.publish(
                new PublishCcctProfileCommand(courseId, profileId, 2)))
                .isInstanceOf(CcctConflictException.class);

        verify(savePort, never()).publishDraft(any(), any(), anyLong(), any());
    }

    @Test
    void publishesDraftAndReturnsReloadedPublishedProfile() {
        UUID courseId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        Course course = Course.builder().id(courseId).build();
        CcctProfile draft = CcctProfile.builder()
                .id(profileId)
                .status(CcctProfileStatus.DRAFT)
                .version(3)
                .build();
        CcctProfile published = CcctProfile.builder()
                .id(profileId)
                .status(CcctProfileStatus.PUBLISHED)
                .version(4)
                .build();
        when(access.requireCourseTeacher(courseId)).thenReturn(context(course));
        when(profiles.require(courseId, profileId)).thenReturn(draft);
        when(savePort.publishDraft(
                org.mockito.ArgumentMatchers.eq(courseId),
                org.mockito.ArgumentMatchers.eq(profileId),
                org.mockito.ArgumentMatchers.eq(3L),
                any())).thenReturn(true);
        when(loadPort.findByCourseIdAndId(courseId, profileId))
                .thenReturn(Optional.of(published));

        assertThat(service.publish(
                new PublishCcctProfileCommand(courseId, profileId, 3)))
                .isSameAs(published);
        verify(validator).validatePublish(course, draft);
    }

    private CcctAccess.CourseActor context(Course course) {
        return new CcctAccess.CourseActor(
                course,
                new CurrentActor(UUID.randomUUID(), Roles.TEACHER));
    }
}
