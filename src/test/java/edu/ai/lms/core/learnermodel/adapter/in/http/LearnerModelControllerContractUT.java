package edu.ai.lms.core.learnermodel.adapter.in.http;

import edu.ai.lms.core.learnermodel.app.exception.LearnerModelUpstreamException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LearnerModelControllerContractUT {
    @Test
    void exposesTheEightExplicitCourseScopedRoutes() {
        RequestMapping base = LearnerModelController.class.getAnnotation(RequestMapping.class);
        assertThat(base.value()).containsExactly("/api/{courseId}/learner-model");

        Set<String> paths = Arrays.stream(LearnerModelController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> Arrays.stream(annotation.value()))
                .collect(Collectors.toSet());

        assertThat(paths).containsExactlyInAnyOrder(
                "/dkt-custom/learning-outcomes/{learningOutcomeId}/predict",
                "/dkt-custom/learning-outcomes/predict",
                "/dkt-pykt/learning-outcomes/{learningOutcomeId}/predict",
                "/dkt-pykt/learning-outcomes/predict",
                "/akt-custom/learning-outcomes/{learningOutcomeId}/predict",
                "/akt-custom/learning-outcomes/predict",
                "/akt-pykt/learning-outcomes/{learningOutcomeId}/predict",
                "/akt-pykt/learning-outcomes/predict");
    }

    @Test
    void translatesUnavailableAndInvalidUpstreamFailures() {
        LearnerModelExceptionHandler handler = new LearnerModelExceptionHandler();

        var unavailable = handler.handleUpstream(new LearnerModelUpstreamException(
                LearnerModelUpstreamException.Kind.UNAVAILABLE, "timed out", null));
        var invalid = handler.handleUpstream(new LearnerModelUpstreamException(
                LearnerModelUpstreamException.Kind.BAD_RESPONSE, "invalid response", null));

        assertThat(unavailable.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(unavailable.getBody()).isNotNull();
        assertThat(unavailable.getBody().getCode()).isEqualTo("LEARNER_MODEL_UNAVAILABLE");
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(invalid.getBody()).isNotNull();
        assertThat(invalid.getBody().getCode()).isEqualTo("LEARNER_MODEL_BAD_RESPONSE");
    }
}
