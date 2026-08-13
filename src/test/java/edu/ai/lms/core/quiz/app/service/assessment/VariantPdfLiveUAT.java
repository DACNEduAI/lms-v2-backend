package edu.ai.lms.core.quiz.app.service.assessment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository;

/**
 * Explicit local UAT. It is excluded from the normal UT naming convention and
 * runs only when selected with -Dtest=VariantPdfLiveUAT and live dev settings.
 */
@SpringBootTest
@ActiveProfiles("dev")
class VariantPdfLiveUAT {
    @Autowired AssessmentWorkflowRepository workflow;
    @Autowired VariantPdfRenderer renderer;

    @Test
    void rendersTheRequestedSavedVariant() throws Exception {
        UUID assessmentId = UUID.fromString(required("variant.pdf.uat.assessment-id"));
        String variantCode = System.getProperty("variant.pdf.uat.variant-code", "001");
        Path output = Path.of(required("variant.pdf.uat.output")).toAbsolutePath().normalize();
        var variant = workflow.variants(assessmentId).stream()
                .filter(value -> variantCode.equals(value.code()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Live variant code was not found: " + variantCode));

        byte[] pdf = renderer.render(variant);
        Files.createDirectories(output.getParent());
        Files.write(output, pdf);

        assertTrue(pdf.length > 1_000);
        assertTrue(variant.questions().stream().anyMatch(question -> question.content().contains("data-latex")));
        assertTrue(variant.questions().stream().flatMap(question -> question.answers().stream())
                .anyMatch(answer -> answer.content().contains("<img")));
    }

    private String required(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing system property: " + key);
        return value;
    }
}
