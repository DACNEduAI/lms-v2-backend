package edu.ai.lms.core.material.adapter.in.scheduler;

import edu.ai.lms.core.material.app.port.out.persistence.MaterialIndexJobPort;
import edu.ai.lms.core.material.app.port.out.scheduler.ScheduleMaterialIndexJobPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class MaterialIndexRecoveryJob extends QuartzJobBean {
    private final MaterialIndexJobPort jobPort;
    private final ScheduleMaterialIndexJobPort schedulerPort;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        for (var jobId : jobPort.findRecoverableIds(25)) {
            try {
                schedulerPort.schedule(jobId, LocalDateTime.now());
            } catch (RuntimeException ex) {
                log.warn("Could not recover material index job {}", jobId, ex);
            }
        }
    }
}
