package edu.ai.lms.core.quiz3.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.ai.lms.core.quiz3.adapter.out.security.OwaspRichContentSanitizer;
import edu.ai.lms.core.quiz3.app.exception.RichContentValidationException;

class Quiz3RichContentNormalizerUT {
    private Quiz3RichContentNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new Quiz3RichContentNormalizer(new OwaspRichContentSanitizer());
    }

    @Test
    void escapesPlainTextAndPreservesEveryLine() {
        var value = normalizer.normalizeAi("Điều kiện: a < b && b > c\ndòng 2\n");

        assertEquals("TIPTAP_HTML", value.contentFormat().name());
        assertEquals("<p>Điều kiện: a &lt; b &amp;&amp; b &gt; c</p><p>dòng 2</p><p><br></p>", value.content());
    }

    @Test
    void recognizesAndSanitizesSupportedAiHtml() {
        var value = normalizer.normalizeAi("<p>Cây <strong>BST</strong></p><table><tbody><tr><td>7</td></tr></tbody></table>");

        assertTrue(value.content().contains("<strong>BST</strong>"));
        assertTrue(value.content().contains("<table>"));
        assertFalse(value.content().contains("&lt;p&gt;"));
    }

    @Test
    void treatsUnsupportedOrUnownedAiMarkupAsLiteralText() {
        var unsupported = normalizer.normalizeAi("<custom-node>value</custom-node>");
        var externalImage = normalizer.normalizeAi("<p>Diagram</p><img src=\"https://other.test/a.png\">");

        assertTrue(unsupported.content().contains("&lt;custom-node&gt;"));
        assertTrue(externalImage.content().contains("&lt;img src="));
    }

    @Test
    void sanitizesDeclaredHtmlAndRejectsUnownedImages() {
        var value = normalizer.normalizeDeclared(
                "<p onclick=\"bad()\">x<sup>2</sup></p><script>alert(1)</script>",
                "TIPTAP_HTML", source -> false);

        assertTrue(value.content().contains("<sup>2</sup>"));
        assertFalse(value.content().contains("onclick"));
        assertFalse(value.content().contains("script"));
        assertThrows(RichContentValidationException.class, () -> normalizer.normalizeDeclared(
                "<img src=\"https://other.test/a.png\">", "TIPTAP_HTML", source -> false));
    }

    @Test
    void retainsOwnedImagesAndIsIdempotent() {
        String url = "http://lms-public.test/quiz3/rich-content/tree.png";
        var first = normalizer.normalizeDeclared(
                "<p>BST</p><img src=\"" + url + "\" alt=\"Cây BST\" data-image-layout=\"block\">",
                "TIPTAP_HTML", url::equals);
        var second = normalizer.normalizeDeclared(first.content(), first.contentFormat().name(), url::equals);

        assertEquals(first, second);
        assertTrue(first.content().contains("alt=\"Cây BST\""));
        assertTrue(first.content().contains("data-image-layout=\"block\""));
    }

    @Test
    void leavesAmbiguousHistoricalMarkupForManualReview() {
        var result = normalizer.inspectHistorical("<legacy-widget>value</legacy-widget>", "PLAIN_TEXT", source -> false);

        assertFalse(result.convertible());
        assertEquals(Quiz3RichContentNormalizer.HistoricalDecision.AMBIGUOUS_MARKUP, result.decision());
    }
}
