package it.pagopa.pn.national.registries.config.aws;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AwsServicesClientsConfigTest {

    @Test
    void kmsClient() {
        AwsServicesClientsConfig awsServicesClientsConfig = new AwsServicesClientsConfig("eu-south-1");
        assertEquals("kms",awsServicesClientsConfig.kmsClient().serviceName());
    }

    @Test
    void secretsManagerClient() {
        AwsServicesClientsConfig awsServicesClientsConfig = new AwsServicesClientsConfig("eu-south-1");
        assertEquals("secretsmanager",awsServicesClientsConfig.secretsManagerClient().serviceName());
    }

    @Test
    void ssmClient() {
        AwsServicesClientsConfig awsServicesClientsConfig = new AwsServicesClientsConfig("eu-south-1");
        assertEquals("ssm", awsServicesClientsConfig.ssmClient().serviceName());
    }
}
