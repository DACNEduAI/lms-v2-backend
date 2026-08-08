package edu.ai.lms.core.quiz.adapter.in.scheduler;

import edu.ai.lms.core.quiz.app.service.genquiz.QuestionVisualGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionVisualStartupReconciler {
    private final QuestionVisualGenerationService visualGenerationService;

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnce() {
        try {
            visualGenerationService.reconcileSchedules();
        } catch (RuntimeException ex) {
            log.error("Could not reconcile question visual schedules at startup", ex);
        }
    }
}
