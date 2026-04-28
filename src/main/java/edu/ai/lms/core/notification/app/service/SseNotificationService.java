package edu.ai.lms.core.notification.app.service;

import edu.ai.lms.core.notification.app.domain.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseNotificationService {

    // Map lưu trữ: 1 UserID -> Danh sách các SseEmitter (Vì 1 user có thể mở LMS
    // trên nhiều tab/điện thoại)
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Client gọi hàm này để mở kết nối
     */
    public SseEmitter subscribe(UUID userId) {
        // Đặt timeout dài (ví dụ: 30 phút). Nếu để 0 hoặc -1 nó sẽ sống mãi nhưng dễ bị
        // Nginx ngắt.
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        this.emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Xử lý dọn dẹp khi kết nối đứt (User đóng tab, mất mạng...)
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        // Bắn ngay 1 event "connected" mồi để trình duyệt biết kết nối đã thành công và
        // không bị timeout ngay lập tức
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected to LMS Notification Stream"));
        } catch (IOException e) {
            removeEmitter(userId, emitter);
        }

        return emitter;
    }

    /**
     * Hàm này được gọi khi có thông báo mới cần đẩy xuống trình duyệt
     */
    public void pushNotification(UUID userId, Notification notification) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            for (SseEmitter emitter : userEmitters) {
                try {
                    // Đẩy object notification về dạng JSON
                    emitter.send(SseEmitter.event()
                            .name("NEW_NOTIFICATION")
                            .data(notification));
                } catch (IOException e) {
                    log.warn("Failed to push notification to user {}. Removing emitter.", userId);
                    removeEmitter(userId, emitter);
                }
            }
        }
    }

    private void removeEmitter(UUID userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }
}
