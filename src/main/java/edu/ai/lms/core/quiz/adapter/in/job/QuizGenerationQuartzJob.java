package edu.ai.lms.core.quiz.adapter.in.job;

import java.util.UUID;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.service.QuizGenerationProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizGenerationQuartzJob extends QuartzJobBean {

    public static final String JOB_ID_KEY = "jobId";

    private final QuizGenerationProcessingService quizGenerationProcessingService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String jobIdValue = context.getMergedJobDataMap().getString(JOB_ID_KEY);
        if (jobIdValue == null) {
            log.warn("Quiz generation job triggered without jobId");
            return;
        }

        quizGenerationProcessingService.process(UUID.fromString(jobIdValue));
    }
}
