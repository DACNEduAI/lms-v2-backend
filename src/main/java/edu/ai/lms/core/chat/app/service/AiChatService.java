package edu.ai.lms.core.chat.app.service;

import edu.ai.lms.config.CorrelationIdFilter;

import edu.ai.lms.core.chat.adapter.in.http.request.AiChatRequest;
import edu.ai.lms.core.chat.app.domain.AiChatSession;
import edu.ai.lms.core.chat.app.domain.AiCourseChatMessage;
import edu.ai.lms.core.chat.app.port.out.persistence.AiChatPersistencePort;
import edu.ai.lms.core.chat.app.port.out.ValidateChatMaterialsPort;
import edu.ai.lms.core.chat.adapter.in.http.request.ChatSessionDto;
import edu.ai.lms.core.chat.adapter.in.http.request.PythonChatRequest;
import edu.ai.lms.core.course.api.LearningOutcomeCatalogApi;
import edu.ai.lms.core.course.api.LearningOutcomeView;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelType;
import edu.ai.lms.core.learnermodel.app.domain.PredictionStatus;
import edu.ai.lms.core.learnermodel.app.port.in.command.PredictLearnerModelCommand;
import edu.ai.lms.core.learnermodel.app.port.in.usecase.PredictLearnerModelUseCase;
import edu.ai.lms.core.learnermodel.app.exception.AmbiguousLearningOutcomeHistoryException;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelAccessDeniedException;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelUpstreamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final WebClient aiWebClient;
    private final AiChatPersistencePort aiChatPersistencePort;
    private final ValidateChatMaterialsPort validateChatMaterialsPort;
    private final PredictLearnerModelUseCase learnerModelPredictionUseCase;
    private final LearningOutcomeCatalogApi learningOutcomeCatalogApi;

    // ==========================================
    // SESSION MANAGEMENT
    // ==========================================

    /**
     * Tạo một phiên chat mới cho người dùng trong khóa học.
     */
    public ChatSessionDto createSession(UUID userId, UUID courseId) {
        AiChatSession session = AiChatSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .courseId(courseId)
                .title("Đoạn chat mới")
                .isPinned(false)
                .build();

        aiChatPersistencePort.saveSession(session);
        log.info("Created new chat session {} for user {} in course {}", session.getId(), userId, courseId);

        return new ChatSessionDto(session.getId(), session.getTitle(), false, null);
    }

    /**
     * Lấy danh sách phiên chat (hiển thị trên Sidebar bên trái).
     * Sắp xếp: Ghim trước, sau đó mới nhất trước.
     */
    public List<ChatSessionDto> getSessions(UUID userId, UUID courseId) {
        return aiChatPersistencePort.getSessionsByUserAndCourse(userId, courseId);
    }

    /**
     * Ghim hoặc bỏ ghim một phiên chat.
     */
    public void togglePin(UUID courseId, UUID userId, UUID sessionId, boolean isPinned) {
        requireOwnedSession(courseId, userId, sessionId);
        aiChatPersistencePort.updateSessionPin(sessionId, isPinned);
    }

    /**
     * Xóa một phiên chat (cùng toàn bộ tin nhắn bên trong nhờ ON DELETE CASCADE).
     */
    public void deleteSession(UUID courseId, UUID userId, UUID sessionId) {
        requireOwnedSession(courseId, userId, sessionId);
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
        requireOwnedSession(courseId, userId, sessionId);

        // ====== IDOR VALIDATION (Anti-hack) ======
        // Nếu Frontend gửi danh sách materialIds, phải kiểm tra tất cả đều thuộc courseId hiện tại
        List<String> validatedMaterialIds = null;
        if (request.getSelectedMaterialIds() != null && !request.getSelectedMaterialIds().isEmpty()) {
            List<UUID> requestedIds = request.getSelectedMaterialIds();
            if (!validateChatMaterialsPort.allAvailableInCourse(courseId, requestedIds)) {
                log.warn("IDOR attempt detected for user {} and course {}", userId, courseId);
                return Flux.just("{\"token\":\"\",\"done\":true,\"error\":\"Một số tài liệu bạn chọn không thuộc khóa học này hoặc chưa sẵn sàng.\"}");
            }
            
            // Chuyển UUID sang String để gửi sang Python
            validatedMaterialIds = requestedIds.stream()
                    .map(UUID::toString)
                    .collect(java.util.stream.Collectors.toList());
            log.info("Source Selection: {} materials validated for course {}", validatedMaterialIds.size(), courseId);
        }

        // The persisted session is authoritative. Do not accept a client-made
        // history as context because it can diverge from, or impersonate, a chat.
        List<AiChatRequest.ChatHistoryEntry> history = aiChatPersistencePort
                .getMessagesBySession(sessionId).stream()
                .skip(Math.max(0, aiChatPersistencePort.countMessagesBySession(sessionId) - 12L))
                .map(message -> new AiChatRequest.ChatHistoryEntry(message.getRole(), message.getContent()))
                .toList();

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
                .chat_history(history)
                .selected_material_ids(validatedMaterialIds) // Truyền danh sách đã validate sang Python
                .learner_context(request.isPersonalizationEnabled()
                        ? learnerContext(courseId)
                        : List.of())
                .build();

        return aiWebClient.post()
                .uri("/api/chat")
                .header(CorrelationIdFilter.HEADER_NAME, sessionId.toString())
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
    public List<AiCourseChatMessage> getSessionHistory(UUID courseId, UUID userId, UUID sessionId) {
        requireOwnedSession(courseId, userId, sessionId);
        return aiChatPersistencePort.getMessagesBySession(sessionId);
    }

    private void requireOwnedSession(UUID courseId, UUID userId, UUID sessionId) {
        if (!aiChatPersistencePort.sessionBelongsTo(sessionId, userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat session was not found");
        }
    }

    /**
     * Learner-model output is only a cue for the tutor's explanatory style. It
     * is intentionally not treated as a mastery judgement or a retrieval filter.
     */
    private List<String> learnerContext(UUID courseId) {
        try {
            var result = learnerModelPredictionUseCase.predictAll(
                    new PredictLearnerModelCommand(courseId, null, LearnerModelType.AKT_PYKT));
            var candidates = result.predictions().stream()
                    .filter(prediction -> prediction.status() == PredictionStatus.PREDICTED)
                    .filter(prediction -> prediction.probability() != null && prediction.probability() < 0.5d)
                    .sorted(java.util.Comparator.comparingDouble(prediction -> prediction.probability()))
                    .limit(3)
                    .toList();
            if (candidates.isEmpty()) return List.of();

            Map<UUID, LearningOutcomeView> outcomes = learningOutcomeCatalogApi.findByIds(
                    courseId, candidates.stream().map(prediction -> prediction.learningOutcomeId()).toList())
                    .stream().collect(Collectors.toMap(LearningOutcomeView::id, outcome -> outcome));
            return candidates.stream().map(prediction -> {
                LearningOutcomeView outcome = outcomes.get(prediction.learningOutcomeId());
                if (outcome == null) return null;
                String code = outcome.code() == null || outcome.code().isBlank() ? "LO" : outcome.code();
                String description = outcome.description() == null ? "" : outcome.description().trim();
                return description.isEmpty() ? code : code + ": " + description;
            }).filter(java.util.Objects::nonNull).toList();
        } catch (LearnerModelAccessDeniedException exception) {
            // Teachers may use the tutor too, but only students have a learner model.
            return List.of();
        } catch (AmbiguousLearningOutcomeHistoryException | LearnerModelUpstreamException exception) {
            log.warn("Learner context is unavailable for course {}: {}", courseId, exception.getMessage());
            return List.of();
        }
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

}
