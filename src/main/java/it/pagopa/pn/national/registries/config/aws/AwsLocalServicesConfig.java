package it.pagopa.pn.national.registries.config.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.net.URI;

@Configuration
@Slf4j
public class AwsLocalServicesConfig {

    private final String region;

    public AwsLocalServicesConfig(@Value("${aws.region}") String region) {
        this.region = region;
    }


    @Bean
    @Profile("local")
    public KmsClient localKmsClient() {
        return KmsClient.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create("http://localhost:4566"))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    @Profile("local")
    public SecretsManagerClient localSecretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create("http://localhost:4566"))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

}
