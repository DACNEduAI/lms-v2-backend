package edu.ai.lms.core.quiz3.app.domain.richcontent;

public enum RichContentFormat {
    PLAIN_TEXT,
    TIPTAP_HTML;

    public static RichContentFormat fromNullable(String value) {
        if (value == null || value.isBlank()) return PLAIN_TEXT;
        return RichContentFormat.valueOf(value);
    }
}
