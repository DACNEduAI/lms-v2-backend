package edu.ai.lms.core.notification.adapter.out.persistence;

import edu.ai.lms.core.notification.app.domain.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
interface NotificationRepository {
    
    List<Notification> findByUserId(@Param("userId") UUID userId, @Param("offset") long offset, @Param("limit") int limit);
    
    long countByUserIdAndIsReadFalse(@Param("userId") UUID userId);

    long countByUserId(@Param("userId") UUID userId);
    
    Optional<Notification> findById(@Param("id") UUID id);
    
    void insert(Notification notification);
    
    void update(Notification notification);
    
    void markAllAsReadByUserId(@Param("userId") UUID userId);

    default Notification save(Notification notification) {
        if (notification.getId() == null) {
            notification.setId(UUID.randomUUID());
            insert(notification);
        } else {
            int updated = (int) 0; // MyBatis update returns int
            // Logic to check if exists or just try update
            update(notification);
        }
        return findById(notification.getId()).orElseThrow();
    }
}
