package edu.ai.lms.core.notification.adapter.out.persistence;
 
import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.notification.app.domain.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dev")
class NotificationRepositoryTest extends IntegrationTestConfiguration {

    @Autowired
    private NotificationRepository repository;

    @Test
    void shouldSaveAndFindNotification() {
        // Given
        UUID userId = UUID.randomUUID();
        
        Notification notification = Notification.builder()
                .userId(userId)
                .title("Tài liệu mới")
                .message("Giảng viên vừa upload slide chương 1")
                .type("COURSE_UPDATE")
                .linkUrl("/student/courses/123/materials")
                .isRead(false)
                .build();

        // When
        repository.save(notification);

        // Then
        long unreadCount = repository.countByUserIdAndIsReadFalse(userId);
        assertThat(unreadCount).isEqualTo(1);

        List<Notification> list = repository.findByUserId(userId, 0, 10);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTitle()).isEqualTo("Tài liệu mới");
    }
}
