package edu.ai.lms.core.material.adapter.out.storage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Profile("prod")
@Configuration
@ConfigurationProperties("prod.storage")
@Getter
@Setter
public class DigitalOceanConfig {
    @Value("${prod.storage.region}")
    public String region;

    @Value("${prod.storage.accessKey}")
    public String accessKey;

    @Value("${prod.storage.secret}")
    public String secretKey;

    @Value("${prod.storage.endpoint}")
    public String endPoint;

    @Bean
    public S3Client s3Client() {

        return S3Client.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.US_EAST_1)
                .build();
    }
}
