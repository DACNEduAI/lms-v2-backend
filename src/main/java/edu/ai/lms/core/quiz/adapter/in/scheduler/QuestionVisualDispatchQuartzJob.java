package edu.ai.lms.core.quiz.adapter.in.scheduler;

import edu.ai.lms.core.quiz.app.service.genquiz.QuestionVisualGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
@Slf4j
public class QuestionVisualDispatchQuartzJob extends QuartzJobBean {
    private final QuestionVisualGenerationService visualGenerationService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            visualGenerationService.requestDispatch();
        } catch (RuntimeException ex) {
            log.warn("Quartz could not dispatch question visual jobs", ex);
            throw new JobExecutionException("Question visual dispatch failed", ex, true);
        }
    }
}
