package edu.ai.lms.core.asset.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.asset.app.domain.Asset;
import edu.ai.lms.core.asset.app.domain.ImageAsset;
import edu.ai.lms.core.asset.app.exception.AssetNotFoundException;
import edu.ai.lms.core.asset.app.exception.AssetValidationException;
import edu.ai.lms.core.asset.app.port.out.AssetRepository;
import edu.ai.lms.core.course.app.service.CourseTeacherAccess;
import edu.ai.lms.core.material.api.CourseAssetStorageApi;
import edu.ai.lms.core.material.api.StoredObjectMetadata;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageAssetService {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/svg+xml", "image/webp");
    private final AssetRepository assets;
    private final CourseTeacherAccess access;
    private final CourseAssetStorageApi storage;

    @Transactional(readOnly = true)
    public List<ImageAsset> list(UUID courseId) { access.requireCourseTeacher(courseId); return assets.findImages(courseId); }

    @Transactional
    public UploadCreated create(UUID courseId, CreateImage input) {
        access.requireCourseTeacher(courseId);
        validate(input);
        UUID id = UUID.randomUUID();
        String contentType = normalizeContentType(input.contentType());
        String extension = extension(input.originalFileName(), contentType);
        Asset asset = new Asset(id, courseId, Asset.Type.IMAGE, input.name().trim(), input.description().trim(), blankToNull(input.prompt()),
                input.purpose().trim(), normalizeTags(input.tags()), Asset.PublicationStatus.DRAFT, Asset.UploadStatus.PENDING, LocalDateTime.now(), LocalDateTime.now());
        ImageAsset saved = assets.createImage(new ImageAsset(asset, input.originalFileName().trim(), contentType, 0, "courses/" + courseId + "/assets/" + id + "/source." + extension));
        return new UploadCreated(saved, storage.createUploadLink(saved.storageKey(), contentType));
    }

    @Transactional
    public ImageAsset confirm(UUID courseId, UUID assetId) {
        access.requireCourseTeacher(courseId);
        ImageAsset image = require(courseId, assetId);
        StoredObjectMetadata object = storage.inspect(image.storageKey());
        Asset updated = image.asset();
        if (!object.exists() || object.contentLength() <= 0 || object.contentLength() > MAX_BYTES || !sameContentType(image.contentType(), object.contentType())) {
            updated = new Asset(updated.id(), updated.courseId(), updated.type(), updated.name(), updated.description(), updated.prompt(), updated.purpose(), updated.tags(), updated.publicationStatus(), Asset.UploadStatus.FAILED, updated.createdAt(), LocalDateTime.now());
            return assets.saveImage(new ImageAsset(updated, image.originalFileName(), image.contentType(), 0, image.storageKey()));
        }
        updated = new Asset(updated.id(), updated.courseId(), updated.type(), updated.name(), updated.description(), updated.prompt(), updated.purpose(), updated.tags(), updated.publicationStatus(), Asset.UploadStatus.UPLOADED, updated.createdAt(), LocalDateTime.now());
        return assets.saveImage(new ImageAsset(updated, image.originalFileName(), image.contentType(), object.contentLength(), image.storageKey()));
    }

    @Transactional
    public ImageAsset setPublication(UUID courseId, UUID assetId, Asset.PublicationStatus status) {
        access.requireCourseTeacher(courseId);
        ImageAsset image = require(courseId, assetId);
        if (image.asset().uploadStatus() != Asset.UploadStatus.UPLOADED) throw new AssetValidationException("Only uploaded images can be published");
        Asset a = image.asset();
        return assets.saveImage(new ImageAsset(new Asset(a.id(), a.courseId(), a.type(), a.name(), a.description(), a.prompt(), a.purpose(), a.tags(), status, a.uploadStatus(), a.createdAt(), LocalDateTime.now()), image.originalFileName(), image.contentType(), image.byteSize(), image.storageKey()));
    }

    @Transactional
    public void delete(UUID courseId, UUID assetId) {
        access.requireCourseTeacher(courseId);
        ImageAsset image = require(courseId, assetId);
        storage.delete(image.storageKey());
        if (!assets.delete(courseId, assetId)) throw new AssetNotFoundException("Asset not found");
    }

    @Transactional(readOnly = true)
    public String downloadUrl(UUID courseId, UUID assetId) { access.requireCourseTeacher(courseId); return storage.createDownloadLink(require(courseId, assetId).storageKey()); }
    private ImageAsset require(UUID courseId, UUID assetId) { return assets.findImage(courseId, assetId).orElseThrow(() -> new AssetNotFoundException("Image asset not found")); }
    private static void validate(CreateImage i) { if (i.name() == null || i.name().isBlank() || i.name().trim().length() > 255 || i.description() == null || i.description().isBlank() || i.description().trim().length() > 1000 || i.purpose() == null || i.purpose().isBlank() || i.originalFileName() == null || i.originalFileName().isBlank() || !CONTENT_TYPES.contains(normalizeContentType(i.contentType())) || !validExtension(i.originalFileName(), normalizeContentType(i.contentType()))) throw new AssetValidationException("Invalid image asset metadata"); }
    private static String normalizeContentType(String v) { return v == null ? "" : v.trim().toLowerCase(Locale.ROOT); }
    private static boolean sameContentType(String expected, String actual) { return normalizeContentType(expected).equals(normalizeContentType(actual)); }
    private static String extension(String name, String type) { String lower = name.toLowerCase(Locale.ROOT); if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "jpg"; if (lower.endsWith(".gif")) return "gif"; if (lower.endsWith(".svg")) return "svg"; if (lower.endsWith(".webp")) return "webp"; return type.equals("image/png") ? "png" : "bin"; }
    private static boolean validExtension(String name, String type) { String extension = extension(name, type); return switch (type) { case "image/png" -> extension.equals("png"); case "image/jpeg" -> extension.equals("jpg"); case "image/gif" -> extension.equals("gif"); case "image/svg+xml" -> extension.equals("svg"); case "image/webp" -> extension.equals("webp"); default -> false; }; }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static List<String> normalizeTags(List<String> tags) { return tags == null ? List.of() : tags.stream().filter(t -> t != null && !t.isBlank()).map(t -> t.trim().toLowerCase(Locale.ROOT)).distinct().limit(30).toList(); }
    public record CreateImage(String name, String originalFileName, String contentType, String description, String prompt, String purpose, List<String> tags) {}
    public record UploadCreated(ImageAsset image, String presignedUploadUrl) {}
}
