package it.pagopa.pn.national.registries.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {SqsConfiguration.class})
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "aws.region=eu-south-1"
})
class SqsConfigurationTest {

    @Autowired
    private SqsConfiguration sqsConfiguration;

    @Test
    void testSqsConfiguration() {
        Assertions.assertNotNull(this.sqsConfiguration.sqsClient());
    }
}
