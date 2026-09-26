package edu.ai.lms.core.course.app.service.ccct;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.CcctProfile;

@Component
class CcctMarkdownSerializer {

    String serialize(CcctProfile profile) {
        List<String> lines = new ArrayList<>();
        lines.add("# " + profile.getName().trim());
        profile.getChapters().forEach(chapter -> {
            lines.add("");
            lines.add("## " + chapter.getTitle().trim() + " [" + chapter.getChapterCode().trim() + "]");
            for (int itemIndex = 0; itemIndex < chapter.getItems().size(); itemIndex++) {
                var item = chapter.getItems().get(itemIndex);
                String code = item.getContentCode();
                if (code == null || code.isBlank()) code = chapter.getChapterCode().trim() + ".item" + (itemIndex + 1);
                lines.add("- " + item.getContent().trim() + " [" + code.trim() + "]");
            }
        });
        return String.join("\n", lines);
    }
}
