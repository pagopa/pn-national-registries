package it.pagopa.pn.national.registries.config.aws;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import static org.junit.jupiter.api.Assertions.*;

class AwsServicesClientsConfigTest {

    @Test
    void kmsClient() {
        AwsServicesClientsConfig awsServicesClientsConfig = new AwsServicesClientsConfig("eu-south-1");
        Assertions.assertEquals("kms",awsServicesClientsConfig.kmsClient().serviceName());
    }

    @Test
    void secretsManagerClient() {
        AwsServicesClientsConfig awsServicesClientsConfig = new AwsServicesClientsConfig("eu-south-1");
        Assertions.assertEquals("secretsmanager",awsServicesClientsConfig.secretsManagerClient().serviceName());
    }
}
