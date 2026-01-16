package it.pagopa.pn.national.registries.config;

import lombok.CustomLog;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.dynamodb2.DynamoDBLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@Configuration
@CustomLog
public class PnNationalRegistriesSchedulingConfiguration {

    @Bean
    public LockProvider lockProvider(DynamoDbClient dynamoDb, NationalRegistriesConfig cfg) {
        String lockTableName = cfg.getDao().getShedlockTableName();
        log.info("Shared Lock tableName={}", lockTableName);
        return new DynamoDBLockProvider(dynamoDb, lockTableName);
    }
}
