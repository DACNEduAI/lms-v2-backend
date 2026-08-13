package edu.ai.lms.core.material.app.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.domain.Material.MaterialStatus;
import edu.ai.lms.core.material.app.domain.Material.MaterialType;
import edu.ai.lms.core.material.app.port.in.usecase.CleanupStaleMaterialsUseCase;
import edu.ai.lms.core.material.app.port.out.persistence.DeleteMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.storage.VerifyFileUploadedPort;
import lombok.RequiredArgsConstructor;

/**
 * Application service that cleans up stale material rows whose files
 * were never actually uploaded to cloud storage.
 *
 * <p>Timeout thresholds before a PENDING row is eligible for cleanup:
 * <ul>
 *   <li>PDF  – 5 minutes</li>
 *   <li>Other types (VIDEO, etc.) – 20 minutes</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CleanupStaleMaterialsService implements CleanupStaleMaterialsUseCase {

    private static final Logger log = LoggerFactory.getLogger(CleanupStaleMaterialsService.class);

    private static final Duration PDF_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(20);

    private final GetMaterialPort getMaterialPort;
    private final DeleteMaterialPort deleteMaterialPort;
    private final VerifyFileUploadedPort verifyFileUploadedPort;

    @Override
    @Transactional
    public int cleanupStaleMaterials() {
        List<Material> staleCandidates = getMaterialPort.findByStatuses(
                List.of(MaterialStatus.PENDING, MaterialStatus.FAILED));

        LocalDateTime now = LocalDateTime.now(java.time.Clock.systemUTC());
        int deletedCount = 0;

        for (Material material : staleCandidates) {
            // For PENDING materials, skip if still within the timeout threshold
            if (material.getStatus() == MaterialStatus.PENDING && !hasExceededTimeout(material, now)) {
                log.debug("Skipping PENDING material {} – still within timeout window", material.getId());
                continue;
            }

            // Check if the file actually exists in cloud storage
            boolean existsInStorage = verifyFileUploadedPort.isFileUploaded(material.getFileKey());

            if (!existsInStorage) {
                log.info("Deleting stale material {} (status={}, fileKey={})",
                        material.getId(), material.getStatus(), material.getFileKey());
                deleteMaterialPort.deleteById(material.getId());
                deletedCount++;
            } else {
                log.info("File exists for material {} (status={}), skipping deletion",
                        material.getId(), material.getStatus());
            }
        }

        return deletedCount;
    }

    private boolean hasExceededTimeout(Material material, LocalDateTime now) {
        Duration timeout = (material.getType() == MaterialType.PDF) ? PDF_TIMEOUT : DEFAULT_TIMEOUT;
        LocalDateTime updatedAt = material.getUpdatedAt() != null ? material.getUpdatedAt() : material.getCreatedAt();
        return updatedAt != null && Duration.between(updatedAt, now).compareTo(timeout) >= 0;
    }
}
