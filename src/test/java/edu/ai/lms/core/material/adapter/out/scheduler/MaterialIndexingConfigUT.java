package edu.ai.lms.core.material.adapter.out.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;

import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MaterialIndexingConfigUT {
    @Mock Scheduler scheduler;

    @Test
    void schedulePreservesTheAbsoluteFireTimeAcrossJvmTimeZones() throws Exception {
        TimeZone original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        try {
            MaterialIndexingConfig config = new MaterialIndexingConfig(scheduler);
            Instant fireAt = Instant.parse("2026-08-11T06:30:04.231926Z");

            config.schedule(UUID.randomUUID(), fireAt);

            ArgumentCaptor<Trigger> trigger = ArgumentCaptor.forClass(Trigger.class);
            verify(scheduler).scheduleJob(any(JobDetail.class), trigger.capture());
            assertEquals(Date.from(fireAt), trigger.getValue().getStartTime());
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
