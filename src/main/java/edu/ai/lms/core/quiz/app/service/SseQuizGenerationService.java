package edu.ai.lms.core.quiz.app.service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SseQuizGenerationService {

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID jobId, QuizGenerationStatusResponse initialStatus) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.computeIfAbsent(jobId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(jobId, emitter));
        emitter.onTimeout(() -> removeEmitter(jobId, emitter));
        emitter.onError(error -> removeEmitter(jobId, emitter));

        try {
            emitter.send(SseEmitter.event().name("quiz-generation.status")
                    .id(jobId + ":" + initialStatus.getUpdatedAt()).data(initialStatus));
            if (isTerminal(initialStatus)) {
                emitter.complete();
            }
        } catch (IOException ex) {
            removeEmitter(jobId, emitter);
        }

        return emitter;
    }

    public void pushStatus(UUID jobId, QuizGenerationStatusResponse status) {
        CopyOnWriteArrayList<SseEmitter> jobEmitters = emitters.get(jobId);
        if (jobEmitters == null) {
            return;
        }

        for (SseEmitter emitter : jobEmitters) {
            try {
                emitter.send(SseEmitter.event().name("quiz-generation.status")
                        .id(jobId + ":" + status.getUpdatedAt()).data(status));
                if (isTerminal(status)) {
                    emitter.complete();
                }
            } catch (IOException ex) {
                log.warn("Failed to push quiz generation status for job {}. Removing emitter.", jobId);
                removeEmitter(jobId, emitter);
            }
        }
    }

    @Scheduled(fixedRateString = "${quiz.generation.sse-heartbeat-ms:15000}")
    void heartbeat() {
        emitters.forEach((jobId, jobEmitters) -> jobEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException ex) {
                removeEmitter(jobId, emitter);
            }
        }));
    }

    private boolean isTerminal(QuizGenerationStatusResponse status) {
        return status.getStatus() == edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus.COMPLETED
                || status.getStatus() == edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus.FAILED
                || status.getStatus() == edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus.TIMED_OUT;
    }

    private void removeEmitter(UUID jobId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> jobEmitters = emitters.get(jobId);
        if (jobEmitters != null) {
            jobEmitters.remove(emitter);
            if (jobEmitters.isEmpty()) {
                emitters.remove(jobId);
            }
        }
    }
}
