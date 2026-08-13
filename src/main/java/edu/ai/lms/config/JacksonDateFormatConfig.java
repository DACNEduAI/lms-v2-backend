package edu.ai.lms.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppDateFormatProperties.class)
public class JacksonDateFormatConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer dateTimeCustomizer(AppDateFormatProperties properties) {
        DateTimeFormatter localDateFormatter = DateTimeFormatter.ofPattern(properties.localDate());
        DateTimeFormatter localDateTimeFormatter = DateTimeFormatter.ofPattern(properties.localDateTime());

        return builder -> {
            builder.serializers(new LocalDateSerializer(localDateFormatter));
            builder.deserializers(new LocalDateDeserializer(localDateFormatter));
            builder.serializers(new LocalDateTimeSerializer(localDateTimeFormatter));
            builder.deserializers(new LocalDateTimeDeserializer(localDateTimeFormatter));
        };
    }
}