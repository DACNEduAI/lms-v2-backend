package edu.ai.lms.core.quiz3.adapter.in.scheduler;

import edu.ai.lms.core.quiz3.app.domain.PendingQuizTarget;
import edu.ai.lms.core.quiz3.app.port.out.LoadPendingQuizDeadlinePort;
import edu.ai.lms.core.quiz3.events.QuizDeadlineApproachingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineReminderJob extends QuartzJobBean {

    private final LoadPendingQuizDeadlinePort loadPendingQuizDeadlinePort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("Executing DeadlineReminderJob...");
        LocalDateTime fromTime = LocalDateTime.now().plusHours(23);
        LocalDateTime toTime = LocalDateTime.now().plusHours(24);

        List<PendingQuizTarget> targets = loadPendingQuizDeadlinePort.findStudentsWithApproachingDeadlines(fromTime, toTime);
        
        for (PendingQuizTarget target : targets) {
            QuizDeadlineApproachingEvent event = new QuizDeadlineApproachingEvent(
                    target.quizId(), target.courseId(), target.quizTitle());
            applicationEventPublisher.publishEvent(event);
        }
        log.info("DeadlineReminderJob finished. Pushed reminders to {} students.", targets.size());
    }
}
