package edu.ai.lms.core.quiz.app.service.assessment;

import edu.ai.lms.core.quiz.app.domain.assessment.QuestionAiModels.Anchor;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionAiDocumentServiceUT {
    private final QuestionAiDocumentService documents = new QuestionAiDocumentService();

    @Test
    void protectsAndRestoresArtifactsWithoutRegexParsing() {
        String html = "<p>A <span data-type=\"inline-math\" data-latex=\"x^2\" "
                + "data-ai-artifact-id=\"11111111-1111-1111-1111-111111111111\" "
                + "data-ai-artifact-type=\"LATEX\"></span> B</p><img src=\"/safe.svg\" alt=\"tree\">";

        var projection = documents.protectArtifacts(html);
        String restored = documents.restoreArtifacts(projection.content().replace("A ", "Rõ hơn "), projection);

        var body = Jsoup.parseBodyFragment(restored).body();
        assertThat(body.text()).contains("Rõ hơn", "B");
        assertThat(body.select("[data-latex]")).hasSize(1);
        assertThat(body.select("img")).hasSize(1);
    }

    @Test
    void rejectsAChangedArtifactPlaceholder() {
        var projection = documents.protectArtifacts("<p>A <span data-type=\"inline-math\" data-latex=\"x\"></span></p>");

        assertThatThrownBy(() -> documents.restoreArtifacts("<p>A only</p>", projection))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("placeholder");
    }

    @Test
    void insertsAndEditsLatexAtTextCursorWithStableArtifactId() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String inserted = documents.insertLatex("<p>abcdef</p>", id, "x+1", "INLINE",
                new Anchor("CURSOR", null, 3));
        String edited = documents.updateLatex(inserted, id, "y^2", "BLOCK");

        var body = Jsoup.parseBodyFragment(edited).body();
        assertThat(body.text()).contains("abc", "def");
        assertThat(body.selectFirst("[data-ai-artifact-id='" + id + "']")).isNotNull();
        assertThat(body.selectFirst("[data-ai-artifact-id='" + id + "']").attr("data-latex")).isEqualTo("y^2");
        assertThat(body.selectFirst("div[data-type=block-math]")).isNotNull();
    }

    @Test
    void tagsLegacyLatexOnFirstAiEditWithoutChangingItsSource() {
        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");

        String tagged = documents.tagLegacyLatex(
                "<p><span data-type=\"inline-math\" data-latex=\"a+b\"></span></p>",
                "a+b", "INLINE", id);

        var node = Jsoup.parseBodyFragment(tagged).selectFirst("[data-ai-artifact-id='" + id + "']");
        assertThat(node).isNotNull();
        assertThat(node.attr("data-latex")).isEqualTo("a+b");
    }

    @Test
    void tagsLegacySvgWithoutInspectingItsXml() {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        String tagged = documents.tagLegacySvg("<p><img src=\"https://assets.test/old.svg\"></p>",
                "https://assets.test/old.svg", id);

        var image = Jsoup.parseBodyFragment(tagged).selectFirst("img");
        assertThat(image.attr("data-ai-artifact-id")).isEqualTo(id.toString());
        assertThat(image.attr("src")).isEqualTo("https://assets.test/old.svg");
    }

    @Test
    void insertsNewQuiz3SvgWithTheProvidedDisplayDimensions() {
        UUID id = UUID.fromString("55555555-5555-5555-5555-555555555555");

        String inserted = documents.insertInlineSvg("<p>Question</p>", id, "Queue", "https://assets.test/queue.svg",
                new Anchor("END", null, null), 512, 96);

        var image = Jsoup.parseBodyFragment(inserted).selectFirst("img");
        assertThat(image.attr("data-image-layout")).isEqualTo("inline");
        assertThat(image.attr("width")).isEqualTo("512");
        assertThat(image.attr("height")).isEqualTo("96");
    }
}
