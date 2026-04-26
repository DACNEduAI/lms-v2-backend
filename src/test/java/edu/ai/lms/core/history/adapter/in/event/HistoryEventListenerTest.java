package edu.ai.lms.core.history.adapter.in.event;

import edu.ai.lms.common.event.SystemActivityEvent;
import edu.ai.lms.core.history.app.domain.ActivityLog;
import edu.ai.lms.core.history.app.port.out.SaveActivityLogPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HistoryEventListenerTest {

    @Mock
    private SaveActivityLogPort saveActivityLogPort;

    @InjectMocks
    private HistoryEventListener historyEventListener;

    @Test
    void shouldCatchEventAndSaveActivityLog() {
        // 1. Arrange: Tạo một sự kiện giả lập từ module Quiz
        UUID teacherId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        SystemActivityEvent mockEvent = SystemActivityEvent.builder()
                .userId(teacherId)
                .actionType("CREATE")
                .entityType("QUIZ")
                .entityId(quizId)
                .entityName("Bài test giữa kỳ")
                .description("Giảng viên tạo bài kiểm tra")
                .timestamp(LocalDateTime.now())
                .build();

        // 2. Act: Gọi thẳng vào hàm lắng nghe
        historyEventListener.handleSystemActivityEvent(mockEvent);

        // 3. Assert: Bắt lấy object ActivityLog chuẩn bị được lưu xuống DB
        ArgumentCaptor<ActivityLog> logCaptor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(saveActivityLogPort, times(1)).save(logCaptor.capture()); // Đảm bảo hàm save được gọi 1 lần

        ActivityLog savedLog = logCaptor.getValue();
        assertNotNull(savedLog.getId());
        assertEquals(teacherId, savedLog.getUserId());
        assertEquals("CREATE", savedLog.getActionType());
        assertEquals("QUIZ", savedLog.getEntityType());
        assertEquals("Bài test giữa kỳ", savedLog.getEntityName());
    }
}
