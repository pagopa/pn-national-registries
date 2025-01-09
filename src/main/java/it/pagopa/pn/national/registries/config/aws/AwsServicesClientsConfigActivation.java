package it.pagopa.pn.national.registries.config.aws;

import it.pagopa.pn.commons.configs.RuntimeMode;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pn.commons.configs.aws.AwsServicesClientsConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.net.URI;

@Configuration
public class AwsServicesClientsConfigActivation extends AwsServicesClientsConfig {

    private final AwsConfigs props;
    public AwsServicesClientsConfigActivation(AwsConfigs props) {
        super(props, RuntimeMode.PROD);
        this.props = props;
    }

    @Bean
    public KmsClient kmsClient() {
        return KmsClient.builder()
                .region(Region.of(props.getRegionCode()))
                .endpointOverride(URI.create(props.getEndpointUrl()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.of(props.getRegionCode()))
                .endpointOverride(URI.create(props.getEndpointUrl()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

}