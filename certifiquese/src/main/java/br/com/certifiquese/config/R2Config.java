package br.com.certifiquese.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class R2Config {

    @Bean
    public S3Client r2S3Client(
            @Value("${r2.account-id}") String accountId,
            @Value("${r2.access-key-id}") String accessKeyId,
            @Value("${r2.secret-access-key}") String secretAccessKey,
            @Value("${r2.endpoint:}") String configuredEndpoint,
            @Value("${r2.region:auto}") String region) {

        String endpoint = configuredEndpoint == null || configuredEndpoint.isBlank()
                ? "https://" + accountId + ".r2.cloudflarestorage.com"
                : configuredEndpoint.trim();

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                accessKeyId.trim(),
                secretAccessKey.trim());

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region))
                .serviceConfiguration(serviceConfiguration)
                .build();
    }
}
