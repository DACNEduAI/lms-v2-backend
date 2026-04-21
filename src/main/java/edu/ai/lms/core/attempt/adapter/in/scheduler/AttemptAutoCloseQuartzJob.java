package edu.ai.lms.core.attempt.adapter.in.scheduler;

import edu.ai.lms.core.attempt.app.service.AttemptAutoCloseService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.util.UUID;

@DisallowConcurrentExecution
public class AttemptAutoCloseQuartzJob implements Job {

    public static final String ATTEMPT_ID_KEY = "attemptId";
    private static final String APPLICATION_CONTEXT_KEY = "applicationContext";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String attemptIdRaw = jobDataMap.getString(ATTEMPT_ID_KEY);
        if (attemptIdRaw == null || attemptIdRaw.isBlank()) {
            return;
        }

        try {
            UUID attemptId = UUID.fromString(attemptIdRaw);
            ApplicationContext applicationContext = (ApplicationContext) context.getScheduler()
                    .getContext()
                    .get(APPLICATION_CONTEXT_KEY);
            AttemptAutoCloseService attemptAutoCloseService = applicationContext.getBean(AttemptAutoCloseService.class);
            attemptAutoCloseService.closeAttemptIfStillStarted(attemptId, LocalDateTime.now());
        } catch (Exception ex) {
            throw new JobExecutionException("Failed to execute attempt auto-close job", ex);
        }
    }
}
