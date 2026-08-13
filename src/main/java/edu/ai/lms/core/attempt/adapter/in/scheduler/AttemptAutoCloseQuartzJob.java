package edu.ai.lms.core.attempt.adapter.in.scheduler;

import edu.ai.lms.core.attempt.app.service.AttemptAutoCloseService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.UUID;

@Slf4j
@Component
public class AttemptAutoCloseQuartzJob extends QuartzJobBean {

    public static final String ATTEMPT_ID_KEY = "attemptId";

    private final AttemptAutoCloseService attemptAutoCloseService;

    @Autowired
    public AttemptAutoCloseQuartzJob(AttemptAutoCloseService attemptAutoCloseService) {
        this.attemptAutoCloseService = attemptAutoCloseService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String attemptIdRaw = jobDataMap.getString(ATTEMPT_ID_KEY);
        if (attemptIdRaw == null || attemptIdRaw.isBlank()) {
            return;
        }

        try {
            UUID attemptId = UUID.fromString(attemptIdRaw);
            attemptAutoCloseService.closeAttemptIfStillStarted(attemptId, LocalDateTime.now(Clock.systemUTC()));
            log.info("Executed attempt auto-close job for attemptId={} in the AttemptAutoCloseQuartzJob", attemptId);
        } catch (Exception ex) {
            throw new JobExecutionException("Failed to execute attempt auto-close job", ex);
        }
    }
}
