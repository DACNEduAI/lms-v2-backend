package edu.ai.lms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.format")
public record AppDateFormatProperties(
        String localDate,
        String localDateTime
) {
}