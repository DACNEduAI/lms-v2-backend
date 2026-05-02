package edu.ai.lms.core.notification.adapter.out.persistence;

import edu.ai.lms.core.notification.app.domain.Notification;
import edu.ai.lms.core.notification.app.port.out.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class NotificationPersistenceAdapter implements NotificationPort {

    private final NotificationRepository repository;

    @Override
    public Page<Notification> findByUserId(UUID userId, Pageable pageable) {
        long offset = pageable.getOffset();
        int limit = pageable.getPageSize();
        
        List<Notification> notifications = repository.findByUserId(userId, offset, limit);
        long total = repository.countByUserId(userId);
        
        return new PageImpl<>(notifications, pageable, total);
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        return repository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Notification save(Notification notification) {
        return repository.save(notification);
    }

    @Override
    public void markAllAsReadByUserId(UUID userId) {
        repository.markAllAsReadByUserId(userId);
    }
}
