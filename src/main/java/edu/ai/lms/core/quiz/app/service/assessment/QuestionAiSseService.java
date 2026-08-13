package edu.ai.lms.core.quiz.app.service.assessment;

import edu.ai.lms.core.quiz.app.domain.assessment.QuestionAiModels.JobView;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class QuestionAiSseService {
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID jobId, JobView initial) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        emitters.computeIfAbsent(jobId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(jobId, emitter));
        emitter.onTimeout(() -> remove(jobId, emitter));
        emitter.onError(ignored -> remove(jobId, emitter));
        send(emitter, initial);
        return emitter;
    }

    public void push(JobView value) {
        for (SseEmitter emitter : emitters.getOrDefault(value.id(), new CopyOnWriteArrayList<>())) {
            if (!send(emitter, value)) remove(value.id(), emitter);
        }
    }

    private boolean send(SseEmitter emitter, JobView value) {
        try {
            emitter.send(SseEmitter.event().name("question-ai.status").data(value));
            return true;
        } catch (IOException exception) { return false; }
    }

    private void remove(UUID jobId, SseEmitter emitter) {
        var values = emitters.get(jobId);
        if (values == null) return;
        values.remove(emitter);
        if (values.isEmpty()) emitters.remove(jobId);
    }
}
