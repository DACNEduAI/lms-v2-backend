package edu.ai.lms.core.quiz.adapter.out.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import java.time.Instant;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionVisualQuartzSchedulerAdapterUT {
    @Mock private Scheduler scheduler;
    private QuestionVisualQuartzSchedulerAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        adapter = new QuestionVisualQuartzSchedulerAdapter(scheduler);
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        adapter.ensureDispatchJob();
    }

    @Test
    void immediateDispatchUsesOneShotReadyTrigger() throws Exception {
        UUID parentId = UUID.randomUUID();
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(false);

        adapter.scheduleImmediate(parentId, Instant.now());

        ArgumentCaptor<Trigger> trigger = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(trigger.capture());
        assertEquals(parentId.toString(), trigger.getValue().getKey().getName());
        assertEquals("question-visual-ready", trigger.getValue().getKey().getGroup());
        assertEquals(0, ((SimpleTrigger) trigger.getValue()).getRepeatCount());
    }

    @Test
    void retryReplacesLeaseTriggerWithOneShotRetryTrigger() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(false);

        adapter.scheduleRetry(jobId, Instant.now().plusSeconds(2));

        ArgumentCaptor<Trigger> trigger = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(trigger.capture());
        assertEquals(jobId.toString(), trigger.getValue().getKey().getName());
        assertEquals("question-visual-retry", trigger.getValue().getKey().getGroup());
        assertEquals(0, ((SimpleTrigger) trigger.getValue()).getRepeatCount());
    }

    @Test
    void deadlineUsesStableParentJobAndTrigger() throws Exception {
        UUID parentId = UUID.randomUUID();

        adapter.scheduleDeadline(parentId, Instant.now().plusSeconds(1_200));

        ArgumentCaptor<JobDetail> detail = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> trigger = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(detail.capture(), trigger.capture());
        assertEquals(parentId.toString(), detail.getValue().getKey().getName());
        assertEquals("question-visual-deadline", detail.getValue().getKey().getGroup());
        assertEquals("question-visual-deadline-trigger", trigger.getValue().getKey().getGroup());
        assertEquals(0, ((SimpleTrigger) trigger.getValue()).getRepeatCount());
    }

    @Test
    void triggerPreservesInstantWhenTheJvmUsesAsiaHoChiMinh() throws Exception {
        Instant fireAt = Instant.parse("2026-08-13T19:36:58Z");
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            adapter.scheduleImmediate(UUID.randomUUID(), fireAt);
        } finally {
            TimeZone.setDefault(original);
        }

        ArgumentCaptor<Trigger> trigger = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(trigger.capture());
        assertEquals(java.util.Date.from(fireAt), trigger.getValue().getStartTime());
    }
}
