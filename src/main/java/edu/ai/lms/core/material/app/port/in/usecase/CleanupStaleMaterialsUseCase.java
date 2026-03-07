package edu.ai.lms.core.material.app.port.in.usecase;

/**
 * Use case for cleaning up stale materials that have PENDING or FAILED status
 * but whose files do not actually exist in cloud storage.
 */
public interface CleanupStaleMaterialsUseCase {

    /**
     * Scans all materials with PENDING or FAILED status, checks whether the
     * corresponding file exists in storage, and deletes the database row if not.
     * PENDING materials whose updated_at is within the timeout threshold are skipped.
     *
     * @return the number of stale material rows deleted
     */
    int cleanupStaleMaterials();
}
