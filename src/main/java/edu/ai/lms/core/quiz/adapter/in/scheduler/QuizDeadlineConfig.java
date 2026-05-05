package edu.ai.lms.core.quiz.adapter.in.scheduler;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuizDeadlineConfig {

    @Bean
    public JobDetail deadlineReminderJobDetail() {
        return JobBuilder.newJob(DeadlineReminderJob.class)
                .withIdentity("deadlineReminderJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger deadlineReminderJobTrigger(JobDetail deadlineReminderJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(deadlineReminderJobDetail)
                .withIdentity("deadlineReminderTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?")) 
                .build();
    }
}
