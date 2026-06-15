package it.pagopa.pn.national.registries.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import it.pagopa.pn.national.registries.cache.RedisMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConfigurationProperties(prefix = "pn.national-registries")
@Data
@Import(SharedAutoConfiguration.class)
public class NationalRegistriesConfig {

    private String pfNewWorkflowStart;

    private String pfNewWorkflowStop;

    private Dao dao;

    private String addressCompositionMode;

    @Data
    public static class Dao {
        private String shedlockTableName;
    }

    private CacheConfigs redisCache;

    @Data
    public static class CacheConfigs {
        private String hostName;
        private int port;
        private String userId;
        private String cacheName;
        private String cacheRegion;
        private RedisMode mode;
    }

}
