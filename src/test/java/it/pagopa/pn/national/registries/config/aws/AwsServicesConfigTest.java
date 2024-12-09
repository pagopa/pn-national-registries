package it.pagopa.pn.national.registries.config.aws;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AwsServicesConfigTest {

    @Test
    void kmsClient() {
        AwsServicesConfig awsServicesConfig = new AwsServicesConfig("eu-south-1");
        assertEquals("kms", awsServicesConfig.kmsClient().serviceName());
    }

    @Test
    void secretsManagerClient() {
        AwsServicesConfig awsServicesConfig = new AwsServicesConfig("eu-south-1");
        assertEquals("secretsmanager", awsServicesConfig.secretsManagerClient().serviceName());
    }

}
