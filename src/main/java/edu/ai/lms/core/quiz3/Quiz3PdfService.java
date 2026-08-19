package edu.ai.lms.core.quiz3;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AnswerView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.ReviewLearningOutcome;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantQuestionMetadata;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantQuestionView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Quiz3PdfService {
    private final Quiz3AssessmentService assessments;
    private final Quiz3PdfRenderer renderer;

    public GeneratedPdf generate(UUID courseId, UUID assessmentId, UUID variantId) {
        var assessment = assessments.get(courseId, assessmentId);
        var source = assessments.variants(courseId, assessmentId).stream().filter(v -> v.id().equals(variantId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Quiz3 variant was not found"));
        List<VariantQuestionView> questions = source.questions().stream().map(question -> new VariantQuestionView(
                question.id(), null, question.content(), format(question.contentFormat()), question.explanation(),
                format(question.explanationFormat()), question.type(), new VariantQuestionMetadata(
                        question.metadata().matrixRowId().toString(), question.metadata().chapter(), "",
                        "", question.metadata().customLearningLevelName(),
                        List.of(new ReviewLearningOutcome(question.metadata().learningOutcomeId(), question.metadata().learningOutcome())),
                        "", question.metadata().customLearningLevelName()),
                question.answers().stream().map(answer -> new AnswerView(null, answer.content(),
                        format(answer.contentFormat()), answer.correct(), answer.number() > Integer.MAX_VALUE ? 0 : (int) answer.number())).toList(),
                question.orderIndex(), question.version())).toList();
        VariantView variant = new VariantView(source.id(), source.quizId(), source.code(), source.kind(),
                source.orderIndex(), source.shuffleSeed(), source.coverageStatus(), null, null, null, questions);
        return new GeneratedPdf(renderer.render(variant), slug(assessment.title()) + "-quiz-" + source.code() + ".pdf");
    }

    private static RichContentFormat format(String value) {
        return "TIPTAP_HTML".equals(value) ? RichContentFormat.TIPTAP_HTML : RichContentFormat.PLAIN_TEXT;
    }
    private static String slug(String value) {
        String normalized = Normalizer.normalize(value == null ? "quiz3" : value, Normalizer.Form.NFD)
                .replace("đ", "d").replace("Đ", "D").replaceAll("\\p{M}+", "");
        return normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }
    public record GeneratedPdf(byte[] bytes, String filename) { }
}
