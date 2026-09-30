package edu.ai.lms.core.asset.app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.asset.app.domain.ImageAsset;
import edu.ai.lms.core.asset.app.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;

/** Keeps publication durable when Qdrant is unavailable; only successful upserts become searchable. */
@Service
@RequiredArgsConstructor
public class AssetVectorOutboxService {
    private final JdbcTemplate jdbc;
    private final AssetRepository assets;
    private final AssetVectorSyncService vectors;

    @Transactional
    public void enqueue(UUID assetId) {
        jdbc.update("""
                INSERT INTO edu.asset_vector_sync_outbox(asset_id) VALUES (?)
                ON CONFLICT(asset_id) DO UPDATE SET next_attempt_at=CURRENT_TIMESTAMP,last_error=NULL,updated_at=CURRENT_TIMESTAMP
                """,assetId);
    }
    @Scheduled(fixedDelayString="${asset.vector-sync.dispatch-delay-ms:5000}")
    public void dispatch() {
        List<UUID> ids=jdbc.query("SELECT asset_id FROM edu.asset_vector_sync_outbox WHERE next_attempt_at<=CURRENT_TIMESTAMP ORDER BY created_at LIMIT 30",(rs,row)->rs.getObject(1,UUID.class));
        ids.forEach(this::sync);
    }
    @Transactional
    public void sync(UUID assetId) {
        try {
            UUID course=jdbc.query("SELECT course_id FROM edu.assets WHERE id=?",rs->rs.next()?rs.getObject(1,UUID.class):null,assetId);
            ImageAsset image=course==null?null:assets.findImage(course,assetId).orElse(null);
            if(image==null) { jdbc.update("DELETE FROM edu.asset_vector_sync_outbox WHERE asset_id=?",assetId); return; }
            vectors.upsert(image);
            jdbc.update("DELETE FROM edu.asset_vector_sync_outbox WHERE asset_id=?",assetId);
        } catch(RuntimeException ex) {
            String message=ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage();
            jdbc.update("UPDATE edu.asset_vector_sync_outbox SET attempt_count=attempt_count+1,last_error=?,next_attempt_at=CURRENT_TIMESTAMP+INTERVAL '30 seconds',updated_at=CURRENT_TIMESTAMP WHERE asset_id=?",message.substring(0,Math.min(1000,message.length())),assetId);
        }
    }
}
