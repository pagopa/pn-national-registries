package it.pagopa.pn.national.registries.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {DynamoConfiguration.class})
@TestPropertySource(properties = {
        "aws.region=eu-south-1"
})
@ExtendWith(SpringExtension.class)
class DynamoConfigurationTest {

    @Autowired
    private DynamoConfiguration dynamoConfiguration;

    /**
     * Method under test: {@link DynamoConfiguration#dynamoDb()}
     */
    @Test
    void testDynamoDb() {
        Assertions.assertNotNull(this.dynamoConfiguration.dynamoDb());
    }
}
