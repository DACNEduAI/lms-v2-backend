package edu.ai.lms.core.quiz3.adapter.in.cli;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz3.app.service.Quiz3RichContentBackfillService;
import lombok.RequiredArgsConstructor;

/** Runs only when explicitly enabled; normal LMS startup never mutates historical rich content. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "quiz3.rich-content-backfill.enabled", havingValue = "true")
public class Quiz3RichContentBackfillCommand implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(Quiz3RichContentBackfillCommand.class);

    private final Quiz3RichContentBackfillService service;
    private final ConfigurableApplicationContext context;

    @Value("${quiz3.rich-content-backfill.apply:false}")
    private boolean apply;
    @Value("${quiz3.rich-content-backfill.batch-size:100}")
    private int batchSize;
    @Value("${quiz3.rich-content-backfill.restore-run:}")
    private String restoreRun;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (restoreRun != null && !restoreRun.isBlank()) {
                log.info("Quiz3 rich-content restore completed: {}", service.restore(UUID.fromString(restoreRun), batchSize));
            } else {
                log.info("Quiz3 rich-content backfill completed: {}", service.run(apply, batchSize));
            }
        } finally {
            SpringApplication.exit(context);
        }
    }
}
