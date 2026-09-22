package edu.ai.lms.core.quiz3.app.service;

import edu.ai.lms.core.quiz3.app.domain.Quiz3Models;
import edu.ai.lms.core.quiz3.adapter.out.persistence.Quiz3RichContentAssetRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import edu.ai.lms.core.quiz3.app.exception.RichContentValidationException;
import edu.ai.lms.core.quiz3.app.port.out.RichContentAssetStoragePort;

class Quiz3RichContentUploadServiceUT {
    @Test
    void storesAiSvgUnderQuiz3Namespace() {
        RichContentAssetStoragePort storage = mock(RichContentAssetStoragePort.class);
        when(storage.publicUrl(any())).thenAnswer(call -> "https://assets/" + call.getArgument(0));
        var uploaded = new Quiz3RichContentUploadService(storage).uploadAiSvg("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1 1\"><path d=\"M0 0\"/></svg>");
        assertThat(uploaded.storageKey()).startsWith("quiz3/rich-content/").endsWith(".svg");
        assertThat(uploaded.publicUrl()).contains("quiz3/rich-content/");
    }

    @Test
    void rejectsUnsafeSvgBeforeStorage() {
        RichContentAssetStoragePort storage = mock(RichContentAssetStoragePort.class);
        assertThatThrownBy(() -> new Quiz3RichContentUploadService(storage).uploadAiSvg("<svg><script>alert(1)</script></svg>"))
                .isInstanceOf(RichContentValidationException.class);
    }
}
