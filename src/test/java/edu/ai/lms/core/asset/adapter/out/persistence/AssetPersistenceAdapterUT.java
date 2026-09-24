package edu.ai.lms.core.asset.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.asset.app.domain.Asset;
import edu.ai.lms.core.asset.app.domain.ImageAsset;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AssetPersistenceAdapterUT {

    @Test
    void serializesTagsAsJsonThatPostgresCanCastToJsonb() throws Exception {
        AssetRepositoryMapper mapper = Mockito.mock(AssetRepositoryMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        AssetPersistenceAdapter adapter = new AssetPersistenceAdapter(mapper, objectMapper);
        UUID courseId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        List<String> tags = List.of("data-structure", "quote \"and comma, tag", "unicode 三");
        Asset asset = new Asset(assetId, courseId, Asset.Type.IMAGE, "Tree", "A tree", null,
                "Lecture material", tags, Asset.PublicationStatus.DRAFT, Asset.UploadStatus.PENDING, now, now);
        ImageAsset image = new ImageAsset(asset, "tree.png", "image/png", 0, "assets/tree.png", null);

        when(mapper.findImage(courseId, assetId)).thenReturn(rowFor(image));

        adapter.createImage(image);

        ArgumentCaptor<AssetImageRow> captured = ArgumentCaptor.forClass(AssetImageRow.class);
        verify(mapper).insertAsset(captured.capture());
        assertThat(objectMapper.readValue(captured.getValue().getTagsJson(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}))
                .containsExactlyElementsOf(tags);
    }

    @Test
    void readsJsonTagsWithoutSplittingCommasOrEscapedQuotes() throws Exception {
        AssetRepositoryMapper mapper = Mockito.mock(AssetRepositoryMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        AssetPersistenceAdapter adapter = new AssetPersistenceAdapter(mapper, objectMapper);
        UUID courseId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        List<String> tags = List.of("one, tag", "a \"quoted\" tag", "unicode 三");
        Asset asset = new Asset(assetId, courseId, Asset.Type.IMAGE, "Tree", "A tree", null,
                "Lecture material", tags, Asset.PublicationStatus.DRAFT, Asset.UploadStatus.UPLOADED, now, now);
        AssetImageRow row = rowFor(new ImageAsset(asset, "tree.png", "image/png", 42, "assets/tree.png", null));
        row.setTagsJson(objectMapper.writeValueAsString(tags));
        when(mapper.findImages(courseId)).thenReturn(List.of(row));

        assertThat(adapter.findImages(courseId).getFirst().asset().tags()).containsExactlyElementsOf(tags);
    }

    private static AssetImageRow rowFor(ImageAsset image) {
        Asset asset = image.asset();
        AssetImageRow row = new AssetImageRow();
        row.setId(asset.id());
        row.setCourseId(asset.courseId());
        row.setType(asset.type().name());
        row.setName(asset.name());
        row.setDescription(asset.description());
        row.setPrompt(asset.prompt());
        row.setPurpose(asset.purpose());
        row.setTagsJson("[]");
        row.setPublicationStatus(asset.publicationStatus().name());
        row.setUploadStatus(asset.uploadStatus().name());
        row.setCreatedAt(asset.createdAt());
        row.setUpdatedAt(asset.updatedAt());
        row.setOriginalFileName(image.originalFileName());
        row.setContentType(image.contentType());
        row.setByteSize(image.byteSize());
        row.setStorageKey(image.storageKey());
        return row;
    }
}
