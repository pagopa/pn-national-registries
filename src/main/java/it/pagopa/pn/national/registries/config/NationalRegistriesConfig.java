package it.pagopa.pn.national.registries.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConfigurationProperties(prefix = "pn.national-registries")
@Data
@Import(SharedAutoConfiguration.class)
public class NationalRegistriesConfig {

    private boolean enablePfPecFallbackFlow;
    private boolean valCxIdEnabled;

    private Dao dao;
    private Integer queryLimit;
    private Inipec inipec;
    private Inad inad;

    private String addressCompositionMode;

    private Integer ttl;

    @Data
    public static class Dao {
        private String shedlockTableName;
    }

    @Data
    public static class Inipec {
        private Integer maxBatchRequestSize;
        private Integer batchRequestMaxRetry;
        private String batchRequestPkSeparator;
        private Integer batchRequestRecoveryAfter;
        private Double pollingFirstAttemptDelaySecondsPerCf;
        private Integer pollingFirstAttemptFixedDelaySeconds;
        private Integer pollingRetryAfter;
        private Integer pollingInProgressRetryAfter;
    }

    @Data
    public static class Inad {
        private Integer maxBatchRequestSize;
        private Integer oldestRequestMaxWaitingSeconds;
        private Integer batchPollingInProgressMaxRetry;
        private Integer batchPollingMaxRetry;
        private Double pollingFirstAttemptDelaySecondsPerCf;
        private Integer pollingFirstAttemptFixedDelaySeconds;
        private Integer pollingRetryAfter;
        private Integer pollingInProgressRetryAfter;
    }
}
