package edu.ai.lms.core.quiz.adapter.out.scheduler;

import java.util.UUID;

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

import edu.ai.lms.core.quiz.adapter.in.job.QuizGenerationQuartzJob;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuizGenerationJobPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuizGenerationQuartzAdapter implements ScheduleQuizGenerationJobPort {

    private static final String JOB_GROUP = "quiz-generation";
    private static final String TRIGGER_GROUP = "quiz-generation-trigger";

    private final Scheduler scheduler;

    @Override
    public void schedule(UUID jobId) {
        JobKey jobKey = JobKey.jobKey(jobId.toString(), JOB_GROUP);
        TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString(), TRIGGER_GROUP);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(QuizGenerationQuartzJob.JOB_ID_KEY, jobId.toString());

        JobDetail jobDetail = JobBuilder
                .newJob(QuizGenerationQuartzJob.class)
                .withIdentity(jobKey)
                .usingJobData(jobDataMap)
                .build();

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobDetail)
                .startNow()
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
}
