package edu.ai.lms.core.chat.app.service;

import edu.ai.lms.core.chat.app.domain.ChatMessage;
import edu.ai.lms.core.chat.app.port.out.ChatPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Unit Tests")
class ChatServiceUT {

    @Mock
    private ChatPersistencePort chatPersistencePort;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatPersistencePort);
    }

    @Test
    @DisplayName("Should fetch chat history between two users")
    void testGetChatHistory() {
        // Arrange
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        ChatMessage msg1 = ChatMessage.builder().id(UUID.randomUUID()).content("Hello").build();
        ChatMessage msg2 = ChatMessage.builder().id(UUID.randomUUID()).content("Hi").build();
        
        Page<ChatMessage> expectedPage = new PageImpl<>(List.of(msg1, msg2));
        
        when(chatPersistencePort.findChatHistory(user1, user2, pageable)).thenReturn(expectedPage);

        // Act
        Page<ChatMessage> result = chatService.getChatHistory(user1, user2, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Hello");
        verify(chatPersistencePort, times(1)).findChatHistory(user1, user2, pageable);
    }

    @Test
    @DisplayName("Should send a message and save to persistence")
    void testSendMessage() {
        // Arrange
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        String content = "How are you?";
        String tempId = "temp-123";

        ChatMessage savedMsg = ChatMessage.builder()
                .id(UUID.randomUUID())
                .senderId(senderId)
                .recipientId(recipientId)
                .content(content)
                .status("SENT")
                .clientTempId(tempId)
                .build();

        when(chatPersistencePort.save(any(ChatMessage.class))).thenReturn(savedMsg);

        // Act
        ChatMessage result = chatService.sendMessage(senderId, recipientId, content, tempId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getStatus()).isEqualTo("SENT");

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatPersistencePort).save(captor.capture());
        
        ChatMessage captured = captor.getValue();
        assertThat(captured.getSenderId()).isEqualTo(senderId);
        assertThat(captured.getRecipientId()).isEqualTo(recipientId);
        assertThat(captured.getContent()).isEqualTo(content);
        assertThat(captured.getStatus()).isEqualTo("SENT");
        assertThat(captured.getClientTempId()).isEqualTo(tempId);
        assertThat(captured.getCreatedAt()).isNotNull();
    }
}
