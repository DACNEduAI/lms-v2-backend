package edu.ai.lms.core.quiz.adapter.in.scheduler;

import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuizGenerationJobPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class QuizGenerationRecoveryJob extends QuartzJobBean {
    private final LoadQuizGenerationJobPort loadJobPort;
    private final SaveQuizGenerationJobPort saveJobPort;
    private final ScheduleQuizGenerationJobPort schedulerPort;

    @Value("${quiz.generation.retention-days:30}")
    private long retentionDays;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        for (var jobId : loadJobPort.findRecoverableIds(25)) {
            try {
                schedulerPort.schedule(jobId, LocalDateTime.now());
            } catch (RuntimeException ex) {
                log.warn("Could not recover quiz generation job {}", jobId, ex);
            }
        }
        int deleted = saveJobPort.deleteTerminalBefore(LocalDateTime.now().minusDays(retentionDays));
        if (deleted > 0) {
            log.info("Removed {} expired quiz generation job records", deleted);
        }
    }
}
