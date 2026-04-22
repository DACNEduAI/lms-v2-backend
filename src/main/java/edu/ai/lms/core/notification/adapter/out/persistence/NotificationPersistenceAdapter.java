package edu.ai.lms.core.notification.adapter.out.persistence;

import edu.ai.lms.core.notification.app.domain.Notification;
import edu.ai.lms.core.notification.app.port.out.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class NotificationPersistenceAdapter implements NotificationPort {

    private final NotificationRepository repository;

    @Override
    public Page<Notification> findByUserId(UUID userId, Pageable pageable) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDomain);
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        return repository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Notification save(Notification notification) {
        NotificationJdbcEntity saved = repository.save(toEntity(notification));
        return toDomain(saved);
    }

    // --- Mappers ---
    private Notification toDomain(NotificationJdbcEntity entity) {
        return Notification.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .linkUrl(entity.getLinkUrl())
                .isRead(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private NotificationJdbcEntity toEntity(Notification domain) {
        return NotificationJdbcEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .title(domain.getTitle())
                .message(domain.getMessage())
                .type(domain.getType())
                .linkUrl(domain.getLinkUrl())
                .isRead(domain.isRead())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
