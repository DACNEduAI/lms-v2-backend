package edu.ai.lms.core.chat.adapter.in.http;

import edu.ai.lms.core.auth.adapter.in.internal.LoadAuthenticatedUserApi;
import edu.ai.lms.core.chat.app.domain.ChatMessage;
import edu.ai.lms.core.chat.app.port.in.usecase.GetChatHistoryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "APIs for user-to-user chat")
@RequiredArgsConstructor
public class TeacherStudentChatHistoryController {

    private final GetChatHistoryUseCase getChatHistoryUseCase;
    private final LoadAuthenticatedUserApi loadAuthenticatedUserApi;

    @GetMapping("/{partnerId}/history")
    @Operation(summary = "Get chat history with another user")
    public ResponseEntity<Page<ChatMessage>> getChatHistory(
            @PathVariable UUID partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID currentUserId = loadAuthenticatedUserApi.getCurrentUserId();
        Page<ChatMessage> history = getChatHistoryUseCase.getChatHistory(currentUserId, partnerId, PageRequest.of(page, size));
        return ResponseEntity.ok(history);
    }
}
