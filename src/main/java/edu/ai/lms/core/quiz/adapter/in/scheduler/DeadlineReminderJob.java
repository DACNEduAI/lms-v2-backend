package edu.ai.lms.core.quiz.adapter.in.scheduler;

import edu.ai.lms.common.event.SystemNotificationEvent;
import edu.ai.lms.core.quiz.app.domain.PendingQuizTarget;
import edu.ai.lms.core.quiz.app.port.out.LoadPendingQuizDeadlinePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
            SystemNotificationEvent event = new SystemNotificationEvent(
                Set.of(target.studentId()),
                "Nhắc nhở hạn chót",
                "Bài kiểm tra '" + target.quizTitle() + "' sẽ đóng cửa trong vòng 24h tới. Hãy hoàn thành ngay!",
                "DEADLINE_REMINDER",
                "/student/quizzes/" + target.courseId() + "/" + target.quizId() + "/take"
            );
            applicationEventPublisher.publishEvent(event);
        }
        log.info("DeadlineReminderJob finished. Pushed reminders to {} students.", targets.size());
    }
}
