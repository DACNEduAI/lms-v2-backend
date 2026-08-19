package edu.ai.lms.core.quiz3;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** In-memory notification channel; polling remains the reliable cross-restart fallback. */
@Service
public class Quiz3QuestionAiSseService {
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID jobId, Quiz3Models.AiJobView initial) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        emitters.computeIfAbsent(jobId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(jobId, emitter));
        emitter.onTimeout(() -> remove(jobId, emitter));
        emitter.onError(ignored -> remove(jobId, emitter));
        send(emitter, initial);
        return emitter;
    }

    public void push(Quiz3Models.AiJobView value) {
        for (SseEmitter emitter : emitters.getOrDefault(value.id(), new CopyOnWriteArrayList<>())) {
            if (!send(emitter, value)) remove(value.id(), emitter);
        }
    }

    private boolean send(SseEmitter emitter, Quiz3Models.AiJobView value) {
        try {
            emitter.send(SseEmitter.event().name("quiz3-question-ai.status").data(value));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void remove(UUID jobId, SseEmitter emitter) {
        var values = emitters.get(jobId);
        if (values == null) return;
        values.remove(emitter);
        if (values.isEmpty()) emitters.remove(jobId);
    }
}
