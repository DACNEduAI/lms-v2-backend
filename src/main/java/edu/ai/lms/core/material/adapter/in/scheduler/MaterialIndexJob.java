package edu.ai.lms.core.material.adapter.in.scheduler;

import edu.ai.lms.core.material.app.service.MaterialIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class MaterialIndexJob extends QuartzJobBean {
    public static final String JOB_ID_KEY = "jobId";
    private final MaterialIndexingService indexingService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        String value = context.getMergedJobDataMap().getString(JOB_ID_KEY);
        if (value == null) {
            log.warn("Material index job triggered without jobId");
            return;
        }
        indexingService.process(UUID.fromString(value));
    }
}
