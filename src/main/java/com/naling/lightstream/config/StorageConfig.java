package com.naling.lightstream.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

@Configuration
public class StorageConfig {
    @Bean
    public S3Client s3Client(@Value("${lightstream.storage.endpoint}") String endpoint,
                             @Value("${lightstream.storage.access-key}") String accessKey,
                             @Value("${lightstream.storage.secret-key}") String secretKey,
                             @Value("${lightstream.storage.region}") String region
                             ){
        return S3Client.builder().endpointOverride(URI.create(endpoint))// send requests to this address http://localhost:9000
                .region(Region.of(region)) // S3 requires a region
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))) // the user and password used to sign every request
                .forcePathStyle(true) // put the bucket in the local host
                .build();
    }

}
