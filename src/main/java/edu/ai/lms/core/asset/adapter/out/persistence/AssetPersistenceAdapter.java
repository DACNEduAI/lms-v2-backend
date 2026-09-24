package edu.ai.lms.core.asset.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import edu.ai.lms.core.asset.app.domain.Asset;
import edu.ai.lms.core.asset.app.domain.ImageAsset;
import edu.ai.lms.core.asset.app.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class AssetPersistenceAdapter implements AssetRepository {
    private final AssetRepositoryMapper mapper;
    private final ObjectMapper objectMapper;
    @Override public ImageAsset createImage(ImageAsset image) { AssetImageRow row = toRow(image); mapper.insertAsset(row); mapper.insertImage(row); return require(image.asset().courseId(), image.asset().id()); }
    @Override public List<ImageAsset> findImages(UUID courseId) { return mapper.findImages(courseId).stream().map(this::toDomain).toList(); }
    @Override public Optional<ImageAsset> findImage(UUID courseId, UUID assetId) { return Optional.ofNullable(mapper.findImage(courseId, assetId)).map(this::toDomain); }
    @Override public ImageAsset saveImage(ImageAsset image) { AssetImageRow row = toRow(image); mapper.updateAsset(row); mapper.updateImage(row); return require(image.asset().courseId(), image.asset().id()); }
    @Override public boolean delete(UUID courseId, UUID assetId) { return mapper.delete(courseId, assetId) == 1; }
    private ImageAsset require(UUID courseId, UUID assetId) { return findImage(courseId, assetId).orElseThrow(); }
    private AssetImageRow toRow(ImageAsset image) { Asset a = image.asset(); AssetImageRow row = new AssetImageRow(); row.setId(a.id()); row.setCourseId(a.courseId()); row.setType(a.type().name()); row.setName(a.name()); row.setDescription(a.description()); row.setPrompt(a.prompt()); row.setPurpose(a.purpose()); row.setTagsJson(toJson(a.tags())); row.setPublicationStatus(a.publicationStatus().name()); row.setUploadStatus(a.uploadStatus().name()); row.setOriginalFileName(image.originalFileName()); row.setContentType(image.contentType()); row.setByteSize(image.byteSize()); row.setStorageKey(image.storageKey()); if (image.pendingReplacement() != null) { row.setPendingOriginalFileName(image.pendingReplacement().originalFileName()); row.setPendingContentType(image.pendingReplacement().contentType()); row.setPendingStorageKey(image.pendingReplacement().storageKey()); } return row; }
    private ImageAsset toDomain(AssetImageRow row) { Asset asset = new Asset(row.getId(), row.getCourseId(), Asset.Type.valueOf(row.getType()), row.getName(), row.getDescription(), row.getPrompt(), row.getPurpose(), fromJson(row.getTagsJson()), Asset.PublicationStatus.valueOf(row.getPublicationStatus()), Asset.UploadStatus.valueOf(row.getUploadStatus()), row.getCreatedAt(), row.getUpdatedAt()); ImageAsset.PendingReplacement pending = row.getPendingStorageKey() == null ? null : new ImageAsset.PendingReplacement(row.getPendingOriginalFileName(), row.getPendingContentType(), row.getPendingStorageKey()); return new ImageAsset(asset, row.getOriginalFileName(), row.getContentType(), row.getByteSize(), row.getStorageKey(), pending); }
    private String toJson(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize asset tags", exception);
        }
    }
    private List<String> fromJson(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize asset tags", exception);
        }
    }
}
