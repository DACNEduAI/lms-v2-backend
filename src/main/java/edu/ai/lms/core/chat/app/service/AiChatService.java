package edu.ai.lms.core.chat.app.service;

import edu.ai.lms.core.chat.adapter.in.http.request.AiChatRequest;
import edu.ai.lms.core.chat.app.domain.AiChatSession;
import edu.ai.lms.core.chat.app.domain.AiCourseChatMessage;
import edu.ai.lms.core.chat.app.port.out.persistence.AiChatPersistencePort;
import edu.ai.lms.core.chat.adapter.in.http.request.IngestRequest;
import edu.ai.lms.core.chat.adapter.out.python.dto.PythonChatRequest;
import edu.ai.lms.core.chat.adapter.in.http.response.ChatSessionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final WebClient aiRagWebClient;
    private final AiChatPersistencePort aiChatPersistencePort;

    // ==========================================
    // SESSION MANAGEMENT
    // ==========================================

    /**
     * Tạo một phiên chat mới cho người dùng trong khóa học.
     */
    public ChatSessionResponse createSession(UUID userId, UUID courseId) {
        AiChatSession session = AiChatSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .courseId(courseId)
                .title("Đoạn chat mới")
                .isPinned(false)
                .build();

        aiChatPersistencePort.saveSession(session);
        log.info("Created new chat session {} for user {} in course {}", session.getId(), userId, courseId);

        return new ChatSessionResponse(session.getId(), session.getTitle(), false, null);
    }

    /**
     * Lấy danh sách phiên chat (hiển thị trên Sidebar bên trái).
     * Sắp xếp: Ghim trước, sau đó mới nhất trước.
     */
    public List<ChatSessionResponse> getSessions(UUID userId, UUID courseId) {
        return aiChatPersistencePort.getSessionsByUserAndCourse(userId, courseId);
    }

    /**
     * Ghim hoặc bỏ ghim một phiên chat.
     */
    public void togglePin(UUID sessionId, boolean isPinned) {
        aiChatPersistencePort.updateSessionPin(sessionId, isPinned);
    }

    /**
     * Xóa một phiên chat (cùng toàn bộ tin nhắn bên trong nhờ ON DELETE CASCADE).
     */
    public void deleteSession(UUID sessionId) {
        aiChatPersistencePort.deleteSession(sessionId);
        log.info("Deleted chat session {}", sessionId);
    }

    // ==========================================
    // CHAT STREAMING (Core Logic)
    // ==========================================

    /**
     * Hứng luồng SSE từ Python Server và chuyển tiếp về dạng Flux<String>.
     * Quá trình này hoàn toàn Non-blocking, không chiếm giữ Thread của Tomcat.
     */
    public Flux<String> streamChatWithAi(UUID courseId, UUID userId, UUID sessionId, AiChatRequest request) {
        log.info("Chat request | course={} | session={} | user={}", courseId, sessionId, userId);

        // ====== IDOR VALIDATION (Anti-hack) ======
        // Nếu Frontend gửi danh sách materialIds, phải kiểm tra tất cả đều thuộc courseId hiện tại
        List<String> validatedMaterialIds = null;
        if (request.getSelectedMaterialIds() != null && !request.getSelectedMaterialIds().isEmpty()) {
            List<UUID> requestedIds = request.getSelectedMaterialIds();
            int validCount = aiChatPersistencePort.countValidMaterials(courseId, requestedIds);
            
            if (validCount != requestedIds.size()) {
                log.warn("IDOR attempt detected! User {} sent {} material IDs but only {} are valid for course {}",
                        userId, requestedIds.size(), validCount, courseId);
                return Flux.just("{\"token\":\"\",\"done\":true,\"error\":\"Một số tài liệu bạn chọn không thuộc khóa học này hoặc chưa sẵn sàng.\"}");
            }
            
            // Chuyển UUID sang String để gửi sang Python
            validatedMaterialIds = requestedIds.stream()
                    .map(UUID::toString)
                    .collect(java.util.stream.Collectors.toList());
            log.info("Source Selection: {} materials validated for course {}", validatedMaterialIds.size(), courseId);
        }

        // 1. Lưu câu hỏi của User vào Database (gắn sessionId)
        aiChatPersistencePort.saveMessage(AiCourseChatMessage.builder()
                .sessionId(sessionId)
                .userId(userId)
                .courseId(courseId)
                .role("user")
                .content(request.getUserQuery())
                .build());

        // 2. Auto-title: Nếu đây là tin nhắn ĐẦU TIÊN trong session, lấy nội dung câu hỏi làm tiêu đề
        int messageCount = aiChatPersistencePort.countMessagesBySession(sessionId);
        if (messageCount <= 1) { // Chỉ có tin nhắn user vừa lưu = tin nhắn đầu tiên
            String autoTitle = request.getUserQuery();
            if (autoTitle.length() > 35) {
                autoTitle = autoTitle.substring(0, 35) + "...";
            }
            aiChatPersistencePort.updateSessionTitle(sessionId, autoTitle);
        }

        // 3. Cập nhật thời gian hoạt động cuối cùng của session (để sắp xếp đúng trên Sidebar)
        aiChatPersistencePort.touchSession(sessionId);

        // Đối tượng để gom tích lũy câu trả lời của AI
        StringBuilder fullAiResponse = new StringBuilder();

        PythonChatRequest pythonPayload = PythonChatRequest.builder()
                .course_id(courseId.toString())
                .user_query(request.getUserQuery())
                .chat_history(request.getChatHistory())
                .selected_material_ids(validatedMaterialIds) // Truyền danh sách đã validate sang Python
                .build();

        return aiRagWebClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(pythonPayload)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> {
                    // Trích xuất token từ JSON và cộng dồn vào câu trả lời đầy đủ
                    String token = extractTokenFromJson(chunk);
                    if (token != null && !token.isEmpty()) {
                        fullAiResponse.append(token);
                    }
                })
                .doOnComplete(() -> {
                    // 4. Khi stream kết thúc, lưu toàn bộ câu trả lời của AI vào Database
                    if (fullAiResponse.length() > 0) {
                        aiChatPersistencePort.saveMessage(AiCourseChatMessage.builder()
                                .sessionId(sessionId)
                                .userId(userId)
                                .courseId(courseId)
                                .role("assistant")
                                .content(fullAiResponse.toString())
                                .build());
                    }
                })
                .doOnError(error -> log.error("Lỗi khi kết nối Python AI Server: {}", error.getMessage()))
                .onErrorResume(e -> {
                    String errorJson = "{\"token\":\"\",\"done\":true,\"error\":\"Hệ thống AI đang bảo trì hoặc mất kết nối. Vui lòng thử lại sau.\"}";
                    return Flux.just(errorJson);
                });
    }

    /**
     * Lấy lịch sử chat của MỘT phiên chat cụ thể.
     */
    public List<AiCourseChatMessage> getSessionHistory(UUID sessionId) {
        return aiChatPersistencePort.getMessagesBySession(sessionId);
    }

    // ==========================================
    // UTILITY & EVENT HANDLER
    // ==========================================

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\"\\s*:\\s*\"(.*?)\"");

    private String extractTokenFromJson(String json) {
        if (json == null) return "";
        try {
            Matcher matcher = TOKEN_PATTERN.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.warn("Không thể parse token từ JSON: {}", json);
        }
        return "";
    }

    /**
     * Fire-and-forget: Bắn request nạp tài liệu sang Python rồi giải phóng Thread ngay lập tức,
     * không chờ Python xử lý xong. Được trigger bởi MaterialUploadedForAiEvent.
     */
    @org.springframework.context.event.EventListener
    public void notifyAiServerToIngest(edu.ai.lms.common.event.MaterialUploadedForAiEvent event) {
        UUID courseId = event.getCourseId();
        UUID materialId = event.getMaterialId();
        String fileUrl = event.getFileUrl();
        
        log.info("Triggering background ingestion to AI Server for material: {}", materialId);

        IngestRequest payload = IngestRequest.builder()
                .course_id(courseId.toString())
                .material_id(materialId.toString())
                .file_url(fileUrl)
                .build();

        aiRagWebClient.post()
                .uri("/api/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        success -> log.info("Đã gửi lệnh Ingest thành công tới AI Server cho material {}", materialId),
                        error -> log.error("Gửi lệnh Ingest thất bại cho material {}: {}", materialId, error.getMessage())
                );
    }
}
