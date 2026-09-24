package edu.ai.lms.core.asset.adapter.in;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.asset.app.domain.Asset;
import edu.ai.lms.core.asset.app.domain.ImageAsset;
import edu.ai.lms.core.asset.app.service.ImageAssetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}/assets")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class AssetController {
    private final ImageAssetService images;
    @GetMapping public List<ImageResponse> list(@PathVariable UUID courseId) { return images.list(courseId).stream().map(image -> ImageResponse.from(image, images.downloadUrl(courseId, image.asset().id()))).toList(); }
    @PostMapping("/images") public ResponseEntity<UploadResponse> create(@PathVariable UUID courseId, @Valid @RequestBody CreateImageRequest request) {
        var created = images.create(courseId, new ImageAssetService.CreateImage(request.name(), request.originalFileName(), request.contentType(), request.description(), request.prompt(), request.purpose(), request.tags()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponse(ImageResponse.from(created.image(), null), created.presignedUploadUrl()));
    }
    @PatchMapping("/{assetId}") public ImageResponse update(@PathVariable UUID courseId, @PathVariable UUID assetId, @Valid @RequestBody UpdateImageRequest request) {
        var image = images.update(courseId, assetId, new ImageAssetService.UpdateImage(request.name(), request.description(), request.prompt(), request.purpose(), request.tags()));
        return ImageResponse.from(image, images.downloadUrl(courseId, assetId));
    }
    @PostMapping("/{assetId}/replacement-upload") public ReplacementUploadResponse replacementUpload(@PathVariable UUID courseId, @PathVariable UUID assetId, @Valid @RequestBody ReplacementImageRequest request) {
        var upload = images.beginReplacement(courseId, assetId, new ImageAssetService.ReplacementImage(request.originalFileName(), request.contentType()));
        return new ReplacementUploadResponse(upload.presignedUploadUrl());
    }
    @PatchMapping("/{assetId}/confirm-replacement") public ImageResponse confirmReplacement(@PathVariable UUID courseId, @PathVariable UUID assetId) {
        var image = images.confirmReplacement(courseId, assetId);
        return ImageResponse.from(image, images.downloadUrl(courseId, assetId));
    }
    @PatchMapping("/{assetId}/confirm-upload") public ImageResponse confirm(@PathVariable UUID courseId, @PathVariable UUID assetId) { var image = images.confirm(courseId, assetId); return ImageResponse.from(image, image.asset().uploadStatus() == Asset.UploadStatus.UPLOADED ? images.downloadUrl(courseId, assetId) : null); }
    @PatchMapping("/{assetId}/publication") public ImageResponse publication(@PathVariable UUID courseId, @PathVariable UUID assetId, @Valid @RequestBody PublicationRequest request) { var image = images.setPublication(courseId, assetId, request.status()); return ImageResponse.from(image, images.downloadUrl(courseId, assetId)); }
    @GetMapping("/{assetId}/download-url") public DownloadResponse download(@PathVariable UUID courseId, @PathVariable UUID assetId) { return new DownloadResponse(images.downloadUrl(courseId, assetId)); }
    @DeleteMapping("/{assetId}") public ResponseEntity<Void> delete(@PathVariable UUID courseId, @PathVariable UUID assetId) { images.delete(courseId, assetId); return ResponseEntity.noContent().build(); }

    public record CreateImageRequest(@NotBlank @Size(max = 255) String name, @NotBlank @Size(max = 255) String originalFileName, @NotBlank String contentType, @NotBlank @Size(max = 5000) String description, @Size(max = 5000) String prompt, @NotBlank @Size(max = 5000) String purpose, List<String> tags) {}
    public record UpdateImageRequest(@NotBlank @Size(max = 255) String name, @NotBlank @Size(max = 5000) String description, @Size(max = 5000) String prompt, @NotBlank @Size(max = 5000) String purpose, List<String> tags) {}
    public record ReplacementImageRequest(@NotBlank @Size(max = 255) String originalFileName, @NotBlank String contentType) {}
    public record PublicationRequest(@NotNull Asset.PublicationStatus status) {}
    public record DownloadResponse(String url) {}
    public record UploadResponse(ImageResponse asset, String presignedUploadUrl) {}
    public record ReplacementUploadResponse(String presignedUploadUrl) {}
    public record ImageResponse(UUID id, UUID courseId, String type, String name, String description, String prompt, String purpose, List<String> tags, Asset.PublicationStatus publicationStatus, Asset.UploadStatus uploadStatus, String originalFileName, String contentType, long byteSize, String previewUrl, java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        static ImageResponse from(ImageAsset image, String previewUrl) { Asset a = image.asset(); return new ImageResponse(a.id(), a.courseId(), a.type().name(), a.name(), a.description(), a.prompt(), a.purpose(), a.tags(), a.publicationStatus(), a.uploadStatus(), image.originalFileName(), image.contentType(), image.byteSize(), previewUrl, a.createdAt(), a.updatedAt()); }
    }
}
