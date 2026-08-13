package edu.ai.lms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class JacksonDateFormatConfigUT {

    private final JacksonDateFormatConfig configuration = new JacksonDateFormatConfig();

    @Test
    void emitsLegacyLocalDateTimeAsUtcInstant() throws Exception {
        var mapper = mapper();

        assertThat(mapper.writeValueAsString(new TimestampPayload(LocalDateTime.of(2026, 8, 13, 16, 23))))
                .isEqualTo("{\"at\":\"2026-08-13T16:23:00Z\"}");
    }

    @Test
    void acceptsOnlyTimestampWithOffsetForLegacyLocalDateTime() throws Exception {
        var mapper = mapper();

        var payload = mapper.readValue("{\"at\":\"2026-08-13T23:23:00+07:00\"}", TimestampPayload.class);
        assertThat(payload.at()).isEqualTo(LocalDateTime.of(2026, 8, 13, 16, 23));
        assertThatThrownBy(() -> mapper.readValue("{\"at\":\"2026-08-13T16:23:00\"}", TimestampPayload.class))
                .hasMessageContaining("Expected RFC 3339 timestamp with an explicit UTC offset");
    }

    private com.fasterxml.jackson.databind.ObjectMapper mapper() {
        var builder = new Jackson2ObjectMapperBuilder();
        configuration.dateTimeCustomizer(new AppDateFormatProperties("dd-MM-yyyy", "unused")).customize(builder);
        return builder.build();
    }

    private record TimestampPayload(LocalDateTime at) {
    }
}
