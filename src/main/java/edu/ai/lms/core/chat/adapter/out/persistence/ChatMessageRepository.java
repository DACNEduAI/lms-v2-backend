package edu.ai.lms.core.chat.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface ChatMessageRepository extends CrudRepository<ChatMessageJdbcEntity, UUID>, PagingAndSortingRepository<ChatMessageJdbcEntity, UUID> {

    Page<ChatMessageJdbcEntity> findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByCreatedAtDesc(
            UUID senderId1, UUID recipientId1,
            UUID senderId2, UUID recipientId2,
            Pageable pageable);
}
