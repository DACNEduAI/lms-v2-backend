package edu.ai.lms.core.notification.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

// Package-private để đảm bảo tính đóng gói của module
interface NotificationRepository extends ListCrudRepository<NotificationJdbcEntity, UUID>, PagingAndSortingRepository<NotificationJdbcEntity, UUID> {
    
    Page<NotificationJdbcEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    long countByUserIdAndIsReadFalse(UUID userId);
}
