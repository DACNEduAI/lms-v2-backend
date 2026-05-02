package edu.ai.lms.core.notification.app.service;

import edu.ai.lms.common.event.SystemNotificationEvent;
import edu.ai.lms.core.auth.adapter.in.internal.LoadAuthenticatedUserApi;
import edu.ai.lms.core.notification.adapter.in.http.request.CourseAnnouncementRequest;
import edu.ai.lms.core.notification.app.domain.CourseAnnouncement;
import edu.ai.lms.core.notification.app.port.out.CourseAnnouncementPort;
import edu.ai.lms.core.notification.app.port.out.LoadCourseStudentsPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseAnnouncementServiceUT {

    @Mock
    private LoadCourseStudentsPort loadCourseStudentsPort;
    @Mock
    private CourseAnnouncementPort courseAnnouncementPort;
    @Mock
    private LoadAuthenticatedUserApi loadAuthenticatedUserApi;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CourseAnnouncementService courseAnnouncementService;

    private UUID courseId;
    private UUID teacherId;
    private List<UUID> studentIds;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        studentIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    void shouldSendAnnouncementSuccessfully_WithSpecifiedType() {
        // 1. Arrange
        CourseAnnouncementRequest request = new CourseAnnouncementRequest();
        request.setTitle("Thông báo khẩn cấp");
        request.setMessage("Nghỉ học ngày mai");
        request.setType("URGENT");

        when(loadAuthenticatedUserApi.getCurrentUserId()).thenReturn(teacherId);
        when(loadCourseStudentsPort.findStudentIdsByCourseId(courseId)).thenReturn(studentIds);

        // 2. Act
        courseAnnouncementService.sendAnnouncement(courseId, request);

        // 3. Assert
        // Verify announcement is saved
        ArgumentCaptor<CourseAnnouncement> announcementCaptor = ArgumentCaptor.forClass(CourseAnnouncement.class);
        verify(courseAnnouncementPort).save(announcementCaptor.capture());
        CourseAnnouncement saved = announcementCaptor.getValue();
        assertEquals("URGENT", saved.getType());
        assertEquals(courseId, saved.getCourseId());
        assertEquals(teacherId, saved.getTeacherId());

        // Verify event is published
        ArgumentCaptor<SystemNotificationEvent> eventCaptor = ArgumentCaptor.forClass(SystemNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        SystemNotificationEvent publishedEvent = eventCaptor.getValue();
        assertEquals("URGENT", publishedEvent.type());
        assertEquals(2, publishedEvent.userIds().size());
        assertTrue(publishedEvent.userIds().containsAll(studentIds));
        assertEquals("/student/courses/" + courseId, publishedEvent.linkUrl());
    }

    @Test
    void shouldSendAnnouncementWithDefaultType_WhenTypeIsNull() {
        // 1. Arrange
        CourseAnnouncementRequest request = new CourseAnnouncementRequest();
        request.setTitle("Thông báo bình thường");
        request.setMessage("Nhớ làm bài tập");
        request.setType(null); // Type is null

        when(loadAuthenticatedUserApi.getCurrentUserId()).thenReturn(teacherId);
        when(loadCourseStudentsPort.findStudentIdsByCourseId(courseId)).thenReturn(studentIds);

        // 2. Act
        courseAnnouncementService.sendAnnouncement(courseId, request);

        // 3. Assert
        ArgumentCaptor<CourseAnnouncement> announcementCaptor = ArgumentCaptor.forClass(CourseAnnouncement.class);
        verify(courseAnnouncementPort).save(announcementCaptor.capture());
        assertEquals("INFO", announcementCaptor.getValue().getType());

        ArgumentCaptor<SystemNotificationEvent> eventCaptor = ArgumentCaptor.forClass(SystemNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("INFO", eventCaptor.getValue().type());
    }

    @Test
    void shouldNotPublishEvent_WhenNoStudentsFound() {
        // 1. Arrange
        CourseAnnouncementRequest request = new CourseAnnouncementRequest();
        request.setTitle("Thông báo");
        request.setMessage("Nội dung");

        when(loadAuthenticatedUserApi.getCurrentUserId()).thenReturn(teacherId);
        when(loadCourseStudentsPort.findStudentIdsByCourseId(courseId)).thenReturn(List.of()); // No students

        // 2. Act
        courseAnnouncementService.sendAnnouncement(courseId, request);

        // 3. Assert
        verify(courseAnnouncementPort).save(any(CourseAnnouncement.class));
        verify(eventPublisher, never()).publishEvent(any());
    }
}
