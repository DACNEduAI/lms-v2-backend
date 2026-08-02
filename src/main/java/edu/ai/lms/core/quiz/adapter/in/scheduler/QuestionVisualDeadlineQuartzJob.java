package edu.ai.lms.core.quiz.adapter.in.scheduler;

import edu.ai.lms.core.quiz.app.service.genquiz.QuestionVisualGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
@Slf4j
public class QuestionVisualDeadlineQuartzJob extends QuartzJobBean {
    public static final String PARENT_JOB_ID_KEY = "parentJobId";

    private final QuestionVisualGenerationService visualGenerationService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String parentId = context.getMergedJobDataMap().getString(PARENT_JOB_ID_KEY);
        if (parentId == null) {
            log.warn("Question visual deadline triggered without parentJobId");
            return;
        }
        try {
            visualGenerationService.expireParent(UUID.fromString(parentId));
        } catch (RuntimeException ex) {
            throw new JobExecutionException("Question visual deadline handling failed", ex, false);
        }
    }
}
