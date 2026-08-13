package edu.ai.lms.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
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

        return builder -> {
            builder.serializers(new LocalDateSerializer(localDateFormatter));
            builder.deserializers(new LocalDateDeserializer(localDateFormatter));
            // LocalDateTime remains in legacy DTOs while the database migration is
            // completed. At the HTTP boundary it is always interpreted as UTC and
            // emitted as an explicit RFC 3339 UTC instant.
            builder.serializerByType(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider provider)
                        throws java.io.IOException {
                    generator.writeString(value.toInstant(ZoneOffset.UTC).toString());
                }
            });
            builder.deserializerByType(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonParser parser, DeserializationContext context)
                        throws java.io.IOException {
                    try {
                        return Instant.parse(parser.getText().trim()).atOffset(ZoneOffset.UTC).toLocalDateTime();
                    } catch (RuntimeException ex) {
                        throw context.weirdStringException(parser.getText(), LocalDateTime.class,
                                "Expected RFC 3339 timestamp with an explicit UTC offset");
                    }
                }
            });
        };
    }
}
