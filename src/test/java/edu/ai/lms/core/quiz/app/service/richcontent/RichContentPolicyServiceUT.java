package edu.ai.lms.core.quiz.app.service.richcontent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import edu.ai.lms.core.quiz.adapter.out.security.OwaspRichContentSanitizer;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichTextValue;
import edu.ai.lms.core.quiz.app.exception.RichContentValidationException;

class RichContentPolicyServiceUT {
    private final RichContentPolicyService service = new RichContentPolicyService(new OwaspRichContentSanitizer());

    @Test
    void keepsPlainTextLiteral() {
        var value = service.sanitize(RichTextValue.plainText("a < b & c"), 100);
        assertTrue(value.content().contains("<"));
        assertTrue(value.contentFormat() == RichContentFormat.PLAIN_TEXT);
    }

    @Test
    void stripsExecutableHtmlAndKeepsSafeMathSource() {
        var value = service.sanitize(new RichTextValue(
                "<p onclick=\"alert(1)\">x</p><script>alert(1)</script>"
                        + "<span data-type=\"inline-math\" data-latex=\"x^2\"></span>",
                RichContentFormat.TIPTAP_HTML), 10_000);
        assertFalse(value.content().contains("script"));
        assertFalse(value.content().contains("onclick"));
        assertTrue(value.content().contains("data-latex=\"x^2\""));
    }

    @Test
    void rejectsForbiddenLatexCommands() {
        assertThrows(RichContentValidationException.class, () -> service.sanitize(new RichTextValue(
                "<span data-type=\"inline-math\" data-latex=\"\\\\href{https://bad}{x}\"></span>",
                RichContentFormat.TIPTAP_HTML), 10_000));
    }

    @Test
    void rejectsMalformedOrIncompleteMathNodesWithoutPretendingToParseKatex() {
        assertThrows(RichContentValidationException.class, () -> service.sanitize(new RichTextValue(
                "<span data-type=\"inline-math\"></span>", RichContentFormat.TIPTAP_HTML), 10_000));
        assertThrows(RichContentValidationException.class, () -> service.sanitize(new RichTextValue(
                "<span data-type=\"inline-math\" data-latex=\"   \"></span>", RichContentFormat.TIPTAP_HTML), 10_000));
        assertThrows(RichContentValidationException.class, () -> service.sanitize(new RichTextValue(
                "<span data-type=\"unknown-math\" data-latex=\"x\"></span>", RichContentFormat.TIPTAP_HTML), 10_000));
        assertThrows(RichContentValidationException.class, () -> service.sanitize(new RichTextValue(
                "<span data-type=\"block-math\" data-latex=\"x^{2\"></span>", RichContentFormat.TIPTAP_HTML), 10_000));
        String tooLong = "x".repeat(RichContentPolicyService.MAX_LATEX_LENGTH + 1);
        assertThrows(RichContentValidationException.class, () -> service.sanitize(new RichTextValue(
                "<span data-type=\"inline-math\" data-latex=\"" + tooLong + "\"></span>",
                RichContentFormat.TIPTAP_HTML), 20_000));
    }

    @Test
    void keepsMultilineKatexSourceWhenReadingMathNodesAsDom() {
        String latex = """
                \\int_{0}^{\\infty}
                \\left(
                \\sum_{n=1}^{\\infty}
                \\frac{(-1)^{n+1}x^{2n}}{n!(2n+1)}
                \\right)
                e^{-x^2}\\,dx
                =
                \\frac{\\sqrt{\\pi}}{2}
                \\sum_{n=1}^{\\infty}
                \\frac{(-1)^{n+1}}{n!(2n+1)}
                """;
        var value = service.sanitize(new RichTextValue(
                "<div data-latex=\"" + latex + "\" data-type=\"block-math\"></div>",
                RichContentFormat.TIPTAP_HTML), 20_000);

        var math = Jsoup.parseBodyFragment(value.content()).selectFirst("div[data-type=block-math]");
        assertTrue(math != null);
        assertEquals(latex, math.attr("data-latex"));
    }

    @Test
    void acceptsCommonSafeKatexNotationAndDecodesAttributeEntities() {
        String latex = "\\begin{cases}x^2 & x < 0 \\\\ \\sqrt{x} & x \\ge 0\\end{cases}";
        String htmlSource = latex.replace("&", "&amp;").replace("<", "&lt;");
        var value = service.sanitize(new RichTextValue(
                "<span data-latex=\"" + htmlSource + "\" data-type=\"inline-math\"></span>",
                RichContentFormat.TIPTAP_HTML), 10_000);

        var math = Jsoup.parseBodyFragment(value.content()).selectFirst("span[data-type=inline-math]");
        assertTrue(math != null);
        assertEquals(latex, math.attr("data-latex"));
    }

    @Test
    void forcesSafeRelOnEveryExternalLink() {
        var value = service.sanitize(new RichTextValue("<p><a href=\"https://example.test\">open</a></p>",
                RichContentFormat.TIPTAP_HTML), 10_000);
        assertTrue(value.content().contains("rel=\"noopener noreferrer\""));
    }

    @Test
    void preservesNativeTiptapPixelImageDimensions() {
        var value = service.sanitize(new RichTextValue(
                "<img src=\"https://assets.example/diagram.png\" alt=\"Diagram\" width=\"320\" height=\"180\">",
                RichContentFormat.TIPTAP_HTML), 10_000);

        var image = Jsoup.parseBodyFragment(value.content()).selectFirst("img");
        assertTrue(image != null);
        assertEquals("320", image.attr("width"));
        assertEquals("180", image.attr("height"));
    }

    @Test
    void detectsMathAndImagesAsMeaningfulContent() {
        assertTrue(service.hasMeaningfulContent(new RichTextValue(
                "<span data-type=\"inline-math\" data-latex=\"x\"></span>", RichContentFormat.TIPTAP_HTML)));
        assertTrue(service.hasMeaningfulContent(new RichTextValue("<img src=\"pending-image://1\">", RichContentFormat.TIPTAP_HTML)));
        assertTrue(service.hasMeaningfulContent(new RichTextValue("<p>&nbsp;visible&nbsp;</p>", RichContentFormat.TIPTAP_HTML)));
        assertFalse(service.hasMeaningfulContent(new RichTextValue("<p>&nbsp;</p>", RichContentFormat.TIPTAP_HTML)));
    }
}
