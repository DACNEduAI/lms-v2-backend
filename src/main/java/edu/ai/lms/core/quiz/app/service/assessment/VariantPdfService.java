package edu.ai.lms.core.quiz.app.service.assessment;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AssessmentView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;
import edu.ai.lms.core.quiz.app.exception.VariantPdfNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VariantPdfService {
    private final AssessmentService assessments;
    private final VariantPdfRenderer renderer;

    public GeneratedVariantPdf generate(UUID courseId, UUID assessmentId, UUID variantId) {
        AssessmentView assessment = assessments.get(courseId, assessmentId);
        VariantView variant = assessments.variants(courseId, assessmentId).stream()
                .filter(value -> value.id().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new VariantPdfNotFoundException("Assessment variant was not found"));
        return new GeneratedVariantPdf(renderer.render(variant), filename(assessment.title(), variant.code()));
    }

    static String filename(String assessmentTitle, String variantCode) {
        String title = slug(assessmentTitle);
        String code = slug(variantCode);
        return (title.isBlank() ? "assessment" : title) + "-quiz-" + (code.isBlank() ? "variant" : code) + ".pdf";
    }

    private static String slug(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace("đ", "d").replace("Đ", "D");
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    public record GeneratedVariantPdf(byte[] bytes, String filename) { }
}
