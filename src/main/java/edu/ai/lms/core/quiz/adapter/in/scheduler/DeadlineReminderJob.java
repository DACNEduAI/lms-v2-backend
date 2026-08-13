package edu.ai.lms.core.quiz.adapter.in.scheduler;

import edu.ai.lms.core.quiz.app.domain.PendingQuizTarget;
import edu.ai.lms.core.quiz.app.port.out.LoadPendingQuizDeadlinePort;
import edu.ai.lms.core.quiz.events.QuizDeadlineApproachingEvent;
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
        LocalDateTime fromTime = LocalDateTime.now(java.time.Clock.systemUTC()).plusHours(23);
        LocalDateTime toTime = LocalDateTime.now(java.time.Clock.systemUTC()).plusHours(24);

        List<PendingQuizTarget> targets = loadPendingQuizDeadlinePort.findStudentsWithApproachingDeadlines(fromTime, toTime);
        
        for (PendingQuizTarget target : targets) {
            QuizDeadlineApproachingEvent event = new QuizDeadlineApproachingEvent(
                    target.quizId(), target.courseId(), target.quizTitle());
            applicationEventPublisher.publishEvent(event);
        }
        log.info("DeadlineReminderJob finished. Pushed reminders to {} students.", targets.size());
    }
}
