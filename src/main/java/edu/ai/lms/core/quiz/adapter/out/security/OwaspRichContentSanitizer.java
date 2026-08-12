package edu.ai.lms.core.quiz.adapter.out.security;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.CssSchema;
import org.springframework.stereotype.Component;

import java.util.List;

import edu.ai.lms.core.quiz.app.port.out.SanitizeRichContentPort;

/**
 * The browser may be bypassed. This is the final persistence boundary for
 * Tiptap HTML. The policy intentionally accepts only nodes produced by the
 * editor extension factory.
 */
@Component
public class OwaspRichContentSanitizer implements SanitizeRichContentPort {
    private final PolicyFactory policy = new HtmlPolicyBuilder()
            .allowElements("p", "br", "h1", "h2", "h3", "h4", "h5", "h6", "strong", "em", "u", "s",
                    "code", "pre", "blockquote", "hr", "ul", "ol", "li", "input", "table", "thead",
                    "tbody", "tr", "th", "td", "a", "img", "span", "div")
            .allowAttributes("class").onElements("span", "div", "p", "table", "thead", "tbody", "tr", "th", "td",
                    "ul", "ol", "li", "blockquote", "pre", "code")
            .allowAttributes("style").onElements("span", "p", "h1", "h2", "h3", "h4", "h5", "h6")
            .allowAttributes("href", "target", "rel").onElements("a")
            .allowAttributes("src", "alt", "title").onElements("img")
            .allowAttributes("colspan", "rowspan", "colwidth").onElements("th", "td")
            .allowAttributes("type", "checked", "data-checked").onElements("input", "li")
            .allowAttributes("data-type", "data-latex").onElements("span", "div")
            .allowUrlProtocols("http", "https", "mailto")
            .allowStyling(CssSchema.withProperties(List.of(
                    "color", "background-color", "font-family", "font-size", "line-height", "text-align")))
            .toFactory();

    @Override
    public String sanitize(String html) {
        return policy.sanitize(html == null ? "" : html);
    }
}
