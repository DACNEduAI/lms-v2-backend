package edu.ai.lms.core.material.adapter.out.scheduler;

import edu.ai.lms.core.material.adapter.in.scheduler.MaterialIndexJob;
import edu.ai.lms.core.material.adapter.in.scheduler.MaterialIndexRecoveryJob;
import edu.ai.lms.core.material.app.port.out.scheduler.ScheduleMaterialIndexJobPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class MaterialIndexingConfig implements ScheduleMaterialIndexJobPort {
    private static final String JOB_GROUP = "material-indexing";
    private final Scheduler scheduler;

    @PostConstruct
    void ensureRecoveryJob() {
        JobKey key = JobKey.jobKey("material-index-recovery", "material-index-maintenance");
        try {
            if (scheduler.checkExists(key)) return;
            JobDetail detail = JobBuilder.newJob(MaterialIndexRecoveryJob.class)
                    .withIdentity(key).storeDurably().build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("material-index-recovery-trigger", "material-index-maintenance")
                    .forJob(detail).startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(30)
                            .repeatForever().withMisfireHandlingInstructionNextWithExistingCount())
                    .build();
            scheduler.scheduleJob(detail, trigger);
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to configure material index recovery", ex);
        }
    }

    @Override
    public void schedule(UUID jobId, Instant fireAt) {
        JobKey key = JobKey.jobKey(jobId.toString(), JOB_GROUP);
        JobDataMap data = new JobDataMap();
        data.put(MaterialIndexJob.JOB_ID_KEY, jobId.toString());
        JobDetail detail = JobBuilder.newJob(MaterialIndexJob.class)
                .withIdentity(key).usingJobData(data).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobId.toString(), JOB_GROUP + "-trigger")
                .forJob(detail)
                .startAt(Date.from(fireAt == null ? Instant.now() : fireAt))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
        try {
            if (scheduler.checkExists(key)) scheduler.deleteJob(key);
            scheduler.scheduleJob(detail, trigger);
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to schedule material indexing", ex);
        }
    }
}
