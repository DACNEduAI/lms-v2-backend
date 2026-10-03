package edu.ai.lms.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduled workflow dispatch is disabled while the explicit backfill command owns the process. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "quiz3.rich-content-backfill.enabled", havingValue = "false", matchIfMissing = true)
public class SchedulingConfiguration { }
