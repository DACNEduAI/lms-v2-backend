package edu.ai.lms.core.chat.adapter.in.http;

import edu.ai.lms.core.chat.adapter.in.http.request.AiChatRequest;
import edu.ai.lms.core.chat.app.domain.AiCourseChatMessage;
import edu.ai.lms.core.chat.app.service.AiChatService;
import edu.ai.lms.core.chat.app.service.dto.ChatSessionDto;
import edu.ai.lms.core.chat.app.port.out.persistence.LoadSignedInUserPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/course/{courseId}/ai-chat")
@Tag(name = "AI Chat", description = "Endpoints cho RAG AI Course Chatbot (Multi-session)")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final LoadSignedInUserPort loadSignedInUserPort;

    private UUID getAuthenticatedUserId() {
        return loadSignedInUserPort.getAuthenticatedUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
    }

    // ==========================================
    // SESSION MANAGEMENT APIs (Sidebar)
    // ==========================================

    @Operation(summary = "List chat sessions", description = "Lấy danh sách các phiên chat để hiển thị trên Sidebar.")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionDto>> getSessions(@PathVariable UUID courseId) {
        UUID userId = getAuthenticatedUserId();
        return ResponseEntity.ok(aiChatService.getSessions(userId, courseId));
    }

    @Operation(summary = "Create new chat session", description = "Tạo một đoạn chat mới.")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionDto> createSession(@PathVariable UUID courseId) {
        UUID userId = getAuthenticatedUserId();
        return ResponseEntity.ok(aiChatService.createSession(userId, courseId));
    }

    @Operation(summary = "Pin/Unpin chat session", description = "Ghim hoặc bỏ ghim một đoạn chat.")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    @PutMapping("/sessions/{sessionId}/pin")
    public ResponseEntity<Void> togglePinSession(
            @PathVariable UUID courseId,
            @PathVariable UUID sessionId,
            @RequestParam boolean isPinned) {
        aiChatService.togglePin(sessionId, isPinned);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete chat session", description = "Xóa đoạn chat và toàn bộ tin nhắn bên trong.")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable UUID courseId,
            @PathVariable UUID sessionId) {
        aiChatService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // CHAT APIs (Within a session)
    // ==========================================

    @Operation(summary = "Get session chat history", description = "Lấy lịch sử chat của một phiên chat cụ thể.")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<List<AiCourseChatMessage>> getSessionHistory(
            @PathVariable UUID courseId,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(aiChatService.getSessionHistory(sessionId));
    }

    @Operation(summary = "Stream AI Chat response", description = "Nhận câu hỏi và stream câu trả lời từ AI trong phiên chat cụ thể.")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    @PostMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamAiChat(
            @PathVariable UUID courseId,
            @PathVariable UUID sessionId,
            @RequestBody AiChatRequest request) {

        log.info("AI Chat stream | course={} | session={}", courseId, sessionId);
        UUID userId = getAuthenticatedUserId();

        return aiChatService.streamChatWithAi(courseId, userId, sessionId, request);
    }
}
