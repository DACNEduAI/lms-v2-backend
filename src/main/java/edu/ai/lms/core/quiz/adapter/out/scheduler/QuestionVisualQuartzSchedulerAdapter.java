package edu.ai.lms.core.quiz.adapter.out.scheduler;

import edu.ai.lms.core.quiz.adapter.in.scheduler.QuestionVisualDeadlineQuartzJob;
import edu.ai.lms.core.quiz.adapter.in.scheduler.QuestionVisualDispatchQuartzJob;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuestionVisualJobPort;
import jakarta.annotation.PostConstruct;
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
import java.time.Clock;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class QuestionVisualQuartzSchedulerAdapter implements ScheduleQuestionVisualJobPort {
    private static final JobKey DISPATCH_JOB = JobKey.jobKey("dispatcher", "question-visual");
    private static final String READY_GROUP = "question-visual-ready";
    private static final String RETRY_GROUP = "question-visual-retry";
    private static final String LEASE_GROUP = "question-visual-lease";
    private static final String DEADLINE_JOB_GROUP = "question-visual-deadline";
    private static final String DEADLINE_TRIGGER_GROUP = "question-visual-deadline-trigger";

    private final Scheduler scheduler;

    @PostConstruct
    void ensureDispatchJob() {
        try {
            if (!scheduler.checkExists(DISPATCH_JOB)) {
                JobDetail detail = JobBuilder.newJob(QuestionVisualDispatchQuartzJob.class)
                        .withIdentity(DISPATCH_JOB)
                        .storeDurably()
                        .requestRecovery(true)
                        .build();
                scheduler.addJob(detail, false);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to configure question visual dispatcher", ex);
        }
    }

    @Override
    public void scheduleImmediate(UUID parentJobId, LocalDateTime fireAt) {
        scheduleDispatchTrigger(TriggerKey.triggerKey(parentJobId.toString(), READY_GROUP), fireAt);
    }

    @Override
    public void scheduleRetry(UUID visualJobId, LocalDateTime fireAt) {
        scheduleDispatchTrigger(TriggerKey.triggerKey(visualJobId.toString(), RETRY_GROUP), fireAt);
        cancelTrigger(TriggerKey.triggerKey(visualJobId.toString(), LEASE_GROUP));
    }

    @Override
    public void scheduleLeaseRecovery(UUID visualJobId, LocalDateTime fireAt) {
        scheduleDispatchTrigger(TriggerKey.triggerKey(visualJobId.toString(), LEASE_GROUP), fireAt);
        cancelTrigger(TriggerKey.triggerKey(visualJobId.toString(), RETRY_GROUP));
    }

    @Override
    public void scheduleDeadline(UUID parentJobId, LocalDateTime fireAt) {
        JobKey jobKey = JobKey.jobKey(parentJobId.toString(), DEADLINE_JOB_GROUP);
        TriggerKey triggerKey = TriggerKey.triggerKey(parentJobId.toString(), DEADLINE_TRIGGER_GROUP);
        JobDataMap data = new JobDataMap();
        data.put(QuestionVisualDeadlineQuartzJob.PARENT_JOB_ID_KEY, parentJobId.toString());
        JobDetail detail = JobBuilder.newJob(QuestionVisualDeadlineQuartzJob.class)
                .withIdentity(jobKey)
                .usingJobData(data)
                .requestRecovery(true)
                .build();
        Trigger trigger = oneShot(triggerKey, detail, fireAt);
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            scheduler.scheduleJob(detail, trigger);
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to schedule question visual deadline", ex);
        }
    }

    @Override
    public void cancelChild(UUID visualJobId) {
        cancelTrigger(TriggerKey.triggerKey(visualJobId.toString(), RETRY_GROUP));
        cancelTrigger(TriggerKey.triggerKey(visualJobId.toString(), LEASE_GROUP));
    }

    @Override
    public void cancelParent(UUID parentJobId) {
        cancelTrigger(TriggerKey.triggerKey(parentJobId.toString(), READY_GROUP));
        try {
            JobKey deadline = JobKey.jobKey(parentJobId.toString(), DEADLINE_JOB_GROUP);
            if (scheduler.checkExists(deadline)) {
                scheduler.deleteJob(deadline);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to cancel question visual deadline", ex);
        }
    }

    private void scheduleDispatchTrigger(TriggerKey key, LocalDateTime fireAt) {
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(key)
                .forJob(DISPATCH_JOB)
                .startAt(toDate(fireAt))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
        try {
            if (scheduler.checkExists(key)) {
                scheduler.rescheduleJob(key, trigger);
            } else {
                scheduler.scheduleJob(trigger);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to schedule question visual dispatch", ex);
        }
    }

    private Trigger oneShot(TriggerKey key, JobDetail detail, LocalDateTime fireAt) {
        return TriggerBuilder.newTrigger()
                .withIdentity(key)
                .forJob(detail)
                .startAt(toDate(fireAt))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
    }

    private Date toDate(LocalDateTime fireAt) {
        LocalDateTime value = fireAt == null ? LocalDateTime.now(Clock.systemUTC()) : fireAt;
        return Date.from(value.toInstant(java.time.ZoneOffset.UTC));
    }

    private void cancelTrigger(TriggerKey key) {
        try {
            if (scheduler.checkExists(key)) {
                scheduler.unscheduleJob(key);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to cancel question visual trigger", ex);
        }
    }
}
