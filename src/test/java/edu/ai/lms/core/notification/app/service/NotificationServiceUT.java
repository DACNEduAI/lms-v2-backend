package edu.ai.lms.core.notification.app.service;

import edu.ai.lms.core.notification.app.domain.Notification;
import edu.ai.lms.core.notification.app.port.out.NotificationPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUT {

    @Mock
    private NotificationPort notificationPort; // Giả lập DB

    @InjectMocks
    private NotificationService notificationService; // Class cần test

    @Test
    void shouldMarkAsReadSuccessfully_WhenUserIsOwner() {
        // 1. Arrange
        UUID notificationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Notification mockNotification = Notification.builder()
                .id(notificationId)
                .userId(ownerId)
                .isRead(false)
                .build();

        when(notificationPort.findById(notificationId)).thenReturn(Optional.of(mockNotification));

        // 2. Act
        notificationService.markAsRead(notificationId, ownerId);

        // 3. Assert
        assertTrue(mockNotification.isRead(), "Thông báo phải được chuyển thành đã đọc");
        verify(notificationPort, times(1)).save(mockNotification); // Đảm bảo hàm save được gọi 1 lần
    }

    @Test
    void shouldThrowException_WhenUserIsNotOwner() {
        // 1. Arrange
        UUID notificationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID hackerId = UUID.randomUUID(); // Người dùng khác cố tình truy cập
        
        Notification mockNotification = Notification.builder()
                .id(notificationId)
                .userId(ownerId) // Thuộc về Owner
                .isRead(false)
                .build();

        when(notificationPort.findById(notificationId)).thenReturn(Optional.of(mockNotification));

        // 2. Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            notificationService.markAsRead(notificationId, hackerId); // Hacker gọi hàm
        });

        assertEquals("Unauthorized to read this notification", exception.getMessage());
        verify(notificationPort, never()).save(any()); // Đảm bảo KHÔNG CÓ lệnh save nào xuống DB
    }
}
