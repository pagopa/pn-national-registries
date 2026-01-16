package it.pagopa.pn.national.registries.config;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

class PnNationalRegistriesSchedulingConfigurationTest {

    private PnNationalRegistriesSchedulingConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new PnNationalRegistriesSchedulingConfiguration();
    }

    @Test
    void lockProvider() {
        DynamoDbClient dynamoDB = DynamoDbClient.builder()
                .region(Region.EU_SOUTH_1)
                .build();
        NationalRegistriesConfig cfg = new NationalRegistriesConfig();
        NationalRegistriesConfig.Dao dao = new NationalRegistriesConfig.Dao();
        dao.setShedlockTableName("Lock");
        cfg.setDao(dao);
        LockProvider provider = configuration.lockProvider(dynamoDB, cfg);
        Assertions.assertNotNull(provider);
    }

}