package edu.ai.lms.core.quiz3.app.domain.richcontent;

import java.util.Objects;

public record RichTextValue(String content, RichContentFormat contentFormat) {
    public RichTextValue {
        content = Objects.requireNonNullElse(content, "");
        contentFormat = contentFormat == null ? RichContentFormat.PLAIN_TEXT : contentFormat;
    }

    public static RichTextValue plainText(String content) {
        return new RichTextValue(content, RichContentFormat.PLAIN_TEXT);
    }
}
