package edu.ai.lms.core.quiz.adapter.in.scheduler;

import java.util.UUID;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.service.QuizGenerationProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class QuizGenerationJob extends QuartzJobBean {

    public static final String JOB_ID_KEY = "jobId";

    private final QuizGenerationProcessingService quizGenerationProcessingService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String jobIdValue = context.getMergedJobDataMap().getString(JOB_ID_KEY);
        if (jobIdValue == null) {
            log.warn("Quiz generation job triggered without jobId");
            return;
        }

        try {
            quizGenerationProcessingService.process(UUID.fromString(jobIdValue));
        } catch (Exception ex) {
            throw new JobExecutionException("Quiz generation dispatch failed", ex, false);
        }
    }
}
