package edu.ai.lms.core.quiz3;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AnswerView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.ReviewLearningOutcome;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantQuestionMetadata;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantQuestionView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.api.CodeRunnerContractSnapshotApi;
import edu.ai.lms.core.quiz.api.CodeRunnerContractSnapshotApi.PrintableContract;
import edu.ai.lms.core.quiz3.Quiz3PdfRenderer.AnswerMode;
import edu.ai.lms.core.quiz3.Quiz3PdfRenderer.PrintDocument;
import edu.ai.lms.core.quiz3.Quiz3PdfRenderer.PrintQuestion;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Quiz3PdfService {
    private final Quiz3AssessmentService assessments;
    private final Quiz3PdfRenderer renderer;
    private final CodeRunnerContractSnapshotApi codeRunnerContracts;
    private final JdbcTemplate jdbc;

    public GeneratedPdf generate(UUID courseId, UUID assessmentId, UUID variantId, AnswerMode answerMode) {
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
        CourseLabel course = jdbc.queryForObject(
                "SELECT course_code, course_name FROM edu.courses WHERE id=?",
                (rs, row) -> new CourseLabel(rs.getString(1), rs.getString(2)), courseId);
        List<PrintQuestion> printQuestions = questions.stream().map(question -> new PrintQuestion(
                question,
                "CODE_RUNNER".equals(question.type())
                        ? codeRunnerContracts.findPrintableByQuestionId(question.id()).orElse(null)
                        : null)).toList();
        PrintDocument document = new PrintDocument(
                assessment.title(), assessment.description(), course == null ? "" : course.code(),
                course == null ? "" : course.name(), source.code(), assessment.timeLimitMinutes(),
                answerMode == null ? AnswerMode.ONLINE_SUBMISSION : answerMode, variant, printQuestions);
        return new GeneratedPdf(renderer.render(document), slug(assessment.title()) + "-quiz-" + source.code() + ".pdf");
    }

    /** Backward-compatible default for internal callers. */
    public GeneratedPdf generate(UUID courseId, UUID assessmentId, UUID variantId) {
        return generate(courseId, assessmentId, variantId, AnswerMode.ONLINE_SUBMISSION);
    }

    private static RichContentFormat format(String value) {
        return "TIPTAP_HTML".equals(value) ? RichContentFormat.TIPTAP_HTML : RichContentFormat.PLAIN_TEXT;
    }
    private static String slug(String value) {
        String normalized = Normalizer.normalize(value == null ? "quiz3" : value, Normalizer.Form.NFD)
                .replace("đ", "d").replace("Đ", "D").replaceAll("\\p{M}+", "");
        return normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }
    private record CourseLabel(String code, String name) { }
    public record GeneratedPdf(byte[] bytes, String filename) { }
}
