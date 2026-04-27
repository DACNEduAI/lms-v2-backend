package edu.ai.lms.core.material.adapter.out.storage.config;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public interface StorageConfig {
    public S3Client s3Client();
    public S3Presigner s3Presigner();
    public String getBucketName();
    public String getEndPoint();
}
