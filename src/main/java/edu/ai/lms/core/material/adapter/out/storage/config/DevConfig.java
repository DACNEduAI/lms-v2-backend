package edu.ai.lms.core.material.adapter.out.storage.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Profile("dev")
@Configuration
@ConfigurationProperties("dev.storage")
@Getter
@Setter
public class DevConfig implements StorageConfig{
    public String accessKey;
    public String secretKey;
    public String endPoint;
    public String bucketName;

    @Override
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create("https://s3.localhost.localstack.cloud:4566"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1)
                .build();
    }

    @Override
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create("https://s3.localhost.localstack.cloud:4566"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1)
                .build();
    }
}
