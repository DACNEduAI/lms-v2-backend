package edu.ai.lms.core.quiz.adapter.out.scheduler;

import java.util.UUID;
import java.time.Instant;
import java.util.Date;

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

import edu.ai.lms.core.quiz.adapter.in.scheduler.QuizGenerationJob;
import edu.ai.lms.core.quiz.adapter.in.scheduler.QuizGenerationRecoveryJob;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuizGenerationJobPort;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
class QuizGenerationConfig implements ScheduleQuizGenerationJobPort {

    private static final String JOB_GROUP = "quiz-generation";
    private static final String TRIGGER_GROUP = "quiz-generation-trigger";

    private final Scheduler scheduler;

    @PostConstruct
    void ensureRecoveryJob() {
        JobKey recoveryKey = JobKey.jobKey("quiz-generation-recovery", "quiz-generation-maintenance");
        try {
            if (scheduler.checkExists(recoveryKey)) {
                return;
            }
            JobDetail detail = JobBuilder.newJob(QuizGenerationRecoveryJob.class)
                    .withIdentity(recoveryKey).storeDurably().build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("quiz-generation-recovery-trigger", "quiz-generation-maintenance")
                    .forJob(detail).startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(30)
                            .repeatForever().withMisfireHandlingInstructionNextWithExistingCount())
                    .build();
            scheduler.scheduleJob(detail, trigger);
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to configure quiz generation recovery", ex);
        }
    }

    @Override
    public void schedule(UUID jobId, Instant fireAt) {
        JobKey jobKey = JobKey.jobKey(jobId.toString(), JOB_GROUP);
        TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString(), TRIGGER_GROUP);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(QuizGenerationJob.JOB_ID_KEY, jobId.toString());

        JobDetail jobDetail = JobBuilder
                .newJob(QuizGenerationJob.class)
                .withIdentity(jobKey)
                .usingJobData(jobDataMap)
                .build();

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobDetail)
                .startAt(Date.from(fireAt == null ? Instant.now() : fireAt))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();

        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to schedule quiz generation job", ex);
        }
    }

    @Override
    public void unschedule(UUID jobId) {
        JobKey jobKey = JobKey.jobKey(jobId.toString(), JOB_GROUP);
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to unschedule quiz generation job", ex);
        }
    }
}
