package it.pagopa.pn.national.registries.config.aws;

import it.pagopa.pn.commons.configs.RuntimeMode;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pn.commons.configs.aws.AwsServicesClientsConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

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
        KmsClientBuilder builder = KmsClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create());

        String regionCode = props.getRegionCode();
        if (StringUtils.isNotBlank(regionCode)) {
            builder.region(Region.of(regionCode));
        }

        String endpointUrl = props.getEndpointUrl();
        if (StringUtils.isNotBlank(endpointUrl)) {
            builder.endpointOverride(URI.create(endpointUrl));
        }
        return builder.build();
    }

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        SecretsManagerClientBuilder builder = SecretsManagerClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create());

        String regionCode = props.getRegionCode();
        if (StringUtils.isNotBlank(regionCode)) {
            builder.region(Region.of(regionCode));
        }

        String endpointUrl = props.getEndpointUrl();
        if (StringUtils.isNotBlank(endpointUrl)) {
            builder.endpointOverride(URI.create(endpointUrl));
        }
        return builder.build();
    }

}