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
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;

@Profile("prod")
@Configuration
@ConfigurationProperties("prod.storage")
@Getter
@Setter
public class DigitalOceanConfig implements StorageConfig {
    public String region;
    public String accessKey;
    public String secretKey;
    public String endPoint;
    public String bucketName;

    private boolean corsConfigured;

    @Override
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        S3Client client = S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endPoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();

        ensureCorsConfigured(client);
        return client;
    }

    @Override
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        S3Client client = S3Client.builder()
            .region(Region.of(region))
            .endpointOverride(URI.create(endPoint))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();

        ensureCorsConfigured(client);

        return S3Presigner.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endPoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    private synchronized void ensureCorsConfigured(S3Client client) {
        if (corsConfigured) {
            return;
        }

        CORSRule rule = CORSRule.builder()
            .allowedOrigins("*")
            .allowedHeaders("*")
            .allowedMethods("GET", "PUT", "POST", "DELETE", "HEAD")
            .build();

        PutBucketCorsRequest request = PutBucketCorsRequest.builder()
            .bucket(bucketName)
            .corsConfiguration(CORSConfiguration.builder().corsRules(rule).build())
            .build();

        client.putBucketCors(request);
        corsConfigured = true;
    }
}
