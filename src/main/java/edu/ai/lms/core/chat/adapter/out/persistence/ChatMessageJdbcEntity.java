package edu.ai.lms.core.chat.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_chat_messages")
public class ChatMessageJdbcEntity {
    @Id
    private UUID id;
    private UUID senderId;
    private UUID recipientId;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
