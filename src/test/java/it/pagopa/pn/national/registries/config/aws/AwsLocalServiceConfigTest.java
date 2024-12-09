package it.pagopa.pn.national.registries.config.aws;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AwsLocalServicesConfig.class)
@ActiveProfiles("local")
class AwsLocalServicesConfigTest {

    @Value("${aws.region}")
    private String region;

    @Autowired
    private AwsLocalServicesConfig awsLocalServicesConfig;

    @Test
    void localKmsClient_shouldReturnConfiguredKmsClient() {
        KmsClient kmsClient = awsLocalServicesConfig.localKmsClient();
        assertNotNull(kmsClient);
    }

    @Test
    void localSecretsManagerClient_shouldReturnConfiguredSecretsManagerClient() {
        SecretsManagerClient secretsManagerClient = awsLocalServicesConfig.localSecretsManagerClient();
        assertNotNull(secretsManagerClient);
    }
}