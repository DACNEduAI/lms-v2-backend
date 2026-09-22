package edu.ai.lms.core.quiz3.app.service;

import edu.ai.lms.core.quiz3.app.domain.Quiz3Models;
import edu.ai.lms.core.quiz3.adapter.out.persistence.Quiz3RichContentAssetRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class Quiz3QuestionAiSvgLayoutUT {

    @Test
    void usesTheSvgViewBoxRatioAndCapsOnlyTheLongestDisplaySide() {
        var wide = Quiz3QuestionAiService.svgDisplaySize("<svg viewBox=\"-24 -20 560 104\"></svg>");
        var tall = Quiz3QuestionAiService.svgDisplaySize("<svg viewBox=\"0 0 200 800\"></svg>");

        assertThat(wide.width()).isEqualTo(512);
        assertThat(wide.height()).isEqualTo(95);
        assertThat(tall.width()).isEqualTo(128);
        assertThat(tall.height()).isEqualTo(512);
    }

    @Test
    void rejectsSvgWithoutASafeViewBoxForEditorSizing() {
        assertThatThrownBy(() -> Quiz3QuestionAiService.svgDisplaySize("<svg width=\"900\" height=\"560\"></svg>"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("viewBox");
    }
}
