package edu.ai.lms.core.quiz.app.service.genquiz;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.port.out.QuestionVisualJobPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizGenerationTimingLoggerUT {
    private final Logger logger = (Logger) LoggerFactory.getLogger(QuizGenerationTimingLogger.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private QuizGenerationTimingLogger timingLogger;

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
        timingLogger = new QuizGenerationTimingLogger(new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void logsTextAndWholeQuizDurationsWithoutPayloadContent() {
        Instant startedAt = Instant.now().minusSeconds(2);
        Instant endedAt = startedAt.plusSeconds(2);
        QuizGenerationJob job = QuizGenerationJob.builder()
                .id(UUID.randomUUID()).quizId(UUID.randomUUID())
                .status(QuizGenerationStatus.COMPLETED).phase(QuizGenerationJob.Phase.DONE)
                .attemptCount(1).createdAt(startedAt)
                .requestSnapshot("""
                        {"questionCount":2,"examMatrix":[{"levelCounts":[
                          {"questionCount":2,"imageQuestionCount":1}
                        ]}],"teacherInstruction":"must-not-appear"}
                        """)
                .build();
        QuestionVisualJobPort.Summary summary =
                new QuestionVisualJobPort.Summary(1, 1, 0, 0, startedAt.plusSeconds(1));

        timingLogger.textTerminal(job, "completed", null, endedAt);
        timingLogger.quizTerminal(job, summary, endedAt);

        assertEquals(2, appender.list.size());
        String textLog = appender.list.get(0).getFormattedMessage();
        String quizLog = appender.list.get(1).getFormattedMessage();
        assertTrue(textLog.contains("quiz_generation.text.completed"));
        assertTrue(textLog.contains("duration_ms=2000"));
        assertTrue(textLog.contains("question_count=2 visual_count=1"));
        assertTrue(quizLog.contains("quiz_generation.completed"));
        assertTrue(quizLog.contains("duration_ms=2000"));
        assertTrue(appender.list.stream().noneMatch(event ->
                event.getFormattedMessage().contains("must-not-appear")));
    }

    @Test
    void logsVisualFailureWithQuestionIdentityAndNonNegativeDuration() {
        UUID questionId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        QuestionVisualJobPort.WorkItem work = new QuestionVisualJobPort.WorkItem(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), questionId,
                "AUTO", "secret question", "secret brief", "secret alt", 2,
                startedAt, startedAt.minusSeconds(1));

        timingLogger.visualFailed(work, "VISUAL_SERVICE_ERROR", startedAt.minusSeconds(1));

        assertEquals(2, appender.list.size());
        assertEquals(Level.WARN, appender.list.get(1).getLevel());
        String message = appender.list.get(1).getFormattedMessage();
        assertTrue(message.contains("quiz_generation.visual.failed"));
        assertTrue(message.contains("question_id=" + questionId));
        assertTrue(message.contains("attempts=3 duration_ms=0"));
        assertTrue(appender.list.stream().noneMatch(event ->
                event.getFormattedMessage().contains("secret")));
    }
}
