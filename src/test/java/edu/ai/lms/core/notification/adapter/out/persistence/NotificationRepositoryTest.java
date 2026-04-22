package edu.ai.lms.core.notification.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@ActiveProfiles("test") // Đảm bảo bạn có config test để chạy H2 hoặc Testcontainers
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository repository;

    @Test
    void shouldSaveAndFindNotification() {
        // Given
        UUID userId = UUID.randomUUID(); // Chú ý: Trong thực tế, ID này phải tồn tại trong bảng users nếu có FK Constraint (tùy setup test của bạn)
        
        NotificationJdbcEntity entity = NotificationJdbcEntity.builder()
                .userId(userId)
                .title("Tài liệu mới")
                .message("Giảng viên vừa upload slide chương 1")
                .type("COURSE_UPDATE")
                .linkUrl("/student/courses/123/materials")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // When
        repository.save(entity);

        // Then
        long unreadCount = repository.countByUserIdAndIsReadFalse(userId);
        assertThat(unreadCount).isEqualTo(1);

        Page<NotificationJdbcEntity> page = repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Tài liệu mới");
    }
}
