package edu.ai.lms.core.quiz.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ai.lms.core.quiz.app.service.assessment.AssessmentWebhookService;

class InternalAssessmentWebhookControllerUT {
    private static final String SECRET = "shared-webhook-secret";

    @Test
    void acceptsCurrentCorrectlySignedBody() throws Exception {
        InternalAssessmentWebhookController controller = controller();
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String body = "{\"eventId\":\"event-1\"}";

        boolean valid = (boolean) ReflectionTestUtils.invokeMethod(
                controller, "isValid", timestamp, signature(timestamp, body), body);

        assertThat(valid).isTrue();
    }

    @Test
    void rejectsTamperedOrExpiredCallbacks() throws Exception {
        InternalAssessmentWebhookController controller = controller();
        String current = Long.toString(Instant.now().getEpochSecond());
        String expired = Long.toString(Instant.now().minusSeconds(301).getEpochSecond());

        assertThat((boolean) ReflectionTestUtils.invokeMethod(
                controller, "isValid", current, signature(current, "original"), "tampered")).isFalse();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(
                controller, "isValid", expired, signature(expired, "body"), "body")).isFalse();
    }

    private InternalAssessmentWebhookController controller() {
        InternalAssessmentWebhookController controller = new InternalAssessmentWebhookController(
                mock(AssessmentWebhookService.class), new ObjectMapper());
        ReflectionTestUtils.setField(controller, "webhookSecret", SECRET);
        return controller;
    }

    private String signature(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "v1=" + HexFormat.of().formatHex(mac.doFinal(
                (timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }
}
