package edu.ai.lms.core.quiz.app.service.richcontent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void detectsMathAndImagesAsMeaningfulContent() {
        assertTrue(service.hasMeaningfulContent(new RichTextValue(
                "<span data-type=\"inline-math\" data-latex=\"x\"></span>", RichContentFormat.TIPTAP_HTML)));
        assertTrue(service.hasMeaningfulContent(new RichTextValue("<img src=\"pending-image://1\">", RichContentFormat.TIPTAP_HTML)));
    }
}
