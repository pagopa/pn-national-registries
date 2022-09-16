package it.pagopa.pn.national.registries.config.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
@Slf4j
public class AwsServicesClientsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public KmsClient kmsClient(){
        return KmsClient.builder()
            .region(Region.of(region))
            .build();
    }

    @Bean
    public SecretsManagerClient secretsManagerClient(){
        return SecretsManagerClient.builder()
                .region(Region.of(region))
                .build();
    }

}
