package edu.ai.lms.core.quiz.app.domain.richcontent;

import java.time.LocalDateTime;
import java.util.UUID;

public record RichContentAsset(
        UUID id,
        UUID questionDraftId,
        UUID questionBankQuestionId,
        UUID questionId,
        String storageKey,
        String mediaType,
        long byteSize,
        String sha256,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public RichContentAsset {
        int owners = (questionDraftId == null ? 0 : 1)
                + (questionBankQuestionId == null ? 0 : 1)
                + (questionId == null ? 0 : 1);
        if (owners != 1) throw new IllegalArgumentException("A rich content asset requires exactly one owner");
    }
}
