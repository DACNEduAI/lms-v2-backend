package edu.ai.lms.core.attempt.adapter.out.scheduler;

import edu.ai.lms.core.attempt.adapter.in.scheduler.AttemptAutoCloseQuartzJob;
import edu.ai.lms.core.attempt.app.port.out.ScheduleAttemptAutoClosePort;
import edu.ai.lms.core.attempt.app.port.out.UnscheduleAttemptAutoClosePort;
import lombok.RequiredArgsConstructor;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AttemptAutoCloseSchedulerAdapter implements ScheduleAttemptAutoClosePort, UnscheduleAttemptAutoClosePort {

    private static final String JOB_GROUP = "attempt-auto-close";
    private static final String TRIGGER_GROUP = "attempt-auto-close-trigger";

    private final Scheduler scheduler;

    @Override
    public void schedule(UUID attemptId, LocalDateTime fireAt) {
        if (fireAt == null) {
            return;
        }

        JobKey jobKey = toJobKey(attemptId);
        TriggerKey triggerKey = toTriggerKey(attemptId);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(AttemptAutoCloseQuartzJob.ATTEMPT_ID_KEY, attemptId.toString());

        JobDetail jobDetail = JobBuilder
                .newJob(AttemptAutoCloseQuartzJob.class)
                .withIdentity(jobKey)
                .usingJobData(jobDataMap)
                .build();

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobDetail)
                .startAt(Date.from(fireAt.toInstant(java.time.ZoneOffset.UTC)))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();

        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to schedule attempt auto-close job", ex);
        }
    }

    @Override
    public void unschedule(UUID attemptId) {
        JobKey jobKey = toJobKey(attemptId);
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to unschedule attempt auto-close job", ex);
        }
    }

    private JobKey toJobKey(UUID attemptId) {
        return JobKey.jobKey(attemptId.toString(), JOB_GROUP);
    }

    private TriggerKey toTriggerKey(UUID attemptId) {
        return TriggerKey.triggerKey(attemptId.toString(), TRIGGER_GROUP);
    }
}
