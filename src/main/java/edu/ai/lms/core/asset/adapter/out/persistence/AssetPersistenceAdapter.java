package edu.ai.lms.core.asset.adapter.out.persistence;

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
    @Override public ImageAsset createImage(ImageAsset image) { AssetImageRow row = toRow(image); mapper.insertAsset(row); mapper.insertImage(row); return require(image.asset().courseId(), image.asset().id()); }
    @Override public List<ImageAsset> findImages(UUID courseId) { return mapper.findImages(courseId).stream().map(this::toDomain).toList(); }
    @Override public Optional<ImageAsset> findImage(UUID courseId, UUID assetId) { return Optional.ofNullable(mapper.findImage(courseId, assetId)).map(this::toDomain); }
    @Override public ImageAsset saveImage(ImageAsset image) { AssetImageRow row = toRow(image); mapper.updateAssetLifecycle(row); mapper.updateImageUploadMetadata(row); return require(image.asset().courseId(), image.asset().id()); }
    @Override public boolean delete(UUID courseId, UUID assetId) { return mapper.delete(courseId, assetId) == 1; }
    private ImageAsset require(UUID courseId, UUID assetId) { return findImage(courseId, assetId).orElseThrow(); }
    private AssetImageRow toRow(ImageAsset image) { Asset a = image.asset(); AssetImageRow row = new AssetImageRow(); row.setId(a.id()); row.setCourseId(a.courseId()); row.setType(a.type().name()); row.setName(a.name()); row.setDescription(a.description()); row.setPrompt(a.prompt()); row.setPurpose(a.purpose()); row.setTagsJson(toJson(a.tags())); row.setPublicationStatus(a.publicationStatus().name()); row.setUploadStatus(a.uploadStatus().name()); row.setOriginalFileName(image.originalFileName()); row.setContentType(image.contentType()); row.setByteSize(image.byteSize()); row.setStorageKey(image.storageKey()); return row; }
    private ImageAsset toDomain(AssetImageRow row) { Asset asset = new Asset(row.getId(), row.getCourseId(), Asset.Type.valueOf(row.getType()), row.getName(), row.getDescription(), row.getPrompt(), row.getPurpose(), fromJson(row.getTagsJson()), Asset.PublicationStatus.valueOf(row.getPublicationStatus()), Asset.UploadStatus.valueOf(row.getUploadStatus()), row.getCreatedAt(), row.getUpdatedAt()); return new ImageAsset(asset, row.getOriginalFileName(), row.getContentType(), row.getByteSize(), row.getStorageKey()); }
    private static String toJson(List<String> tags) { return "[" + tags.stream().map(tag -> "\\\"" + tag.replace("\\\"", "\\\\\\\"") + "\\\"").collect(java.util.stream.Collectors.joining(",")) + "]"; }
    private static List<String> fromJson(String value) { if (value == null || value.length() < 2) return List.of(); return java.util.Arrays.stream(value.substring(1, value.length() - 1).split(",")).filter(s -> !s.isBlank()).map(s -> s.trim().replaceAll("^\\\"|\\\"$", "")).toList(); }
}
