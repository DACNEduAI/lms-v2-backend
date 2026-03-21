package edu.ai.lms.core.material.adapter.out.storage.config;

import java.net.URI;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
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

    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endPoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Presigner.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endPoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @PostConstruct
    public void configureCors() {
        log.info("Attempting to set CORS configuration for bucket: {}", bucketName);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        // We use a temporary client in a try-with-resources block to satisfy the IDE warning
        // and cleanly close the HTTP connections without affecting the main Spring @Bean.
        try (S3Client tempClient = S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endPoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()) {

            CORSRule rule = CORSRule.builder()
                    .allowedOrigins("*")
                    .allowedMethods("GET", "PUT", "POST", "DELETE", "HEAD")
                    .allowedHeaders("*")
                    .exposeHeaders("ETag")
                    .maxAgeSeconds(3000)
                    .build();

            CORSConfiguration corsConfiguration = CORSConfiguration.builder()
                    .corsRules(rule)
                    .build();

            PutBucketCorsRequest request = PutBucketCorsRequest.builder()
                    .bucket(bucketName)
                    .corsConfiguration(corsConfiguration)
                    .build();

            tempClient.putBucketCors(request);
            log.info("Successfully applied CORS configuration to DigitalOcean Space.");

        } catch (S3Exception e) {
            log.warn("Failed to set CORS via SDK (Status: {}). DigitalOcean Spaces may reject this SDK's specific payload formatting. Error: {}",
                    e.statusCode(), e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            log.error("Unexpected error applying CORS configuration: ", e);
        }
    }
}
