package edu.ai.lms.core.asset.app.service;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import edu.ai.lms.core.asset.app.domain.Asset;
import edu.ai.lms.core.asset.app.domain.ImageAsset;
import lombok.RequiredArgsConstructor;

/** Synchronous AI vector boundary for published image assets. */
@Service
@RequiredArgsConstructor
public class AssetVectorSyncService {
    private final WebClient aiWebClient;

    public void upsert(ImageAsset image) {
        Asset a = image.asset();
        aiWebClient.post().uri("/assets/images/index").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new IndexRequest(a.courseId(), a.id(), a.type().name(), a.name(), a.description(), a.prompt(),
                        a.tags(), a.publicationStatus().name(), a.uploadStatus().name(), image.originalFileName(),
                        image.contentType(), image.byteSize(), image.storageKey(), isoTimestamp(a.createdAt()), isoTimestamp(a.updatedAt()), image.aiGenerated()))
                .retrieve().toBodilessEntity().block(Duration.ofSeconds(60));
    }

    public void delete(UUID assetId) {
        aiWebClient.post().uri("/assets/images/delete").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new DeleteRequest(assetId)).retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
    }

    private record IndexRequest(UUID courseId, UUID assetId, String type, String name, String description, String prompt,
            List<String> tags, String publicationStatus, String uploadStatus, String originalFileName, String contentType,
            long byteSize, String storageKey, String createdAt, String updatedAt, boolean aiGenerated) { }
    private record DeleteRequest(UUID assetId) { }

    static String isoTimestamp(java.time.LocalDateTime value) {
        return value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
