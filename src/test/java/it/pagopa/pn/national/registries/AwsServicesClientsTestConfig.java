package it.pagopa.pn.national.registries;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration
@Slf4j
public class AwsServicesClientsTestConfig {

    public AwsServicesClientsTestConfig() {

    }

    @Bean
    public KmsClient kmsClient() {
        return KmsClient.builder()
                .region(Region.of("eu-south-1"))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.of("eu-south-1"))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public SsmClient ssmClient() {
        return SsmClient.builder()
                .region(Region.of("eu-south-1"))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
