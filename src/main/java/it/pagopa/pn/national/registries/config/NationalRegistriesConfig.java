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

    private String healthCheckPath;
    private String pfNewWorkflowStart;

    private String pfNewWorkflowStop;
    private String issuer;
    private String environmentType;

    private Dao dao;

    private String addressCompositionMode;
    private Integer customRetryMaxAttempts;
    private boolean valCxIdEnabled;

    private Pdnd pdnd;
    private Ade ade;
    private Anpr anpr;
    private Inad inad;
    private InfoCamere infoCamere;
    private Ipa ipa;

    private String outputQueueName;
    private String inputQueueName;
    private String inputDlqQueueName;

    @Data
    public static class Ipa {
        private String baseUrl;
        private String secret;
    }


    @Data
    public static class InfoCamere {
        private String baseUrl;
        private String clientId;
        private String auth;
        private Integer tokenDeadline;
        private Inipec inipec;
    }

    @Data
    public static class Inipec {
        private Integer ttl;
        private Integer connectionTimeoutMillis;
        private Integer readTimeoutMillis;
        private Integer maxRetryAttempts;
        private boolean retryOnTimeout;
        private Integer batchRequestDelay;
        private Integer batchRequestLockAtMost;
        private Integer batchRequestLockAtLeast;
        private Integer batchRequestMaxRetry; // controllare perchè doppia env
        private String batchRequestPkSeparator;
        private Integer batchPollingDelay; //controllare perchè doppia env
        private Integer batchPollingLockAtMost;
        private Integer batchPollingLockAtLeast;
        private Integer batchPollingMaxRetry; // controllare perchè doppia env
        private Integer batchPollingInProgressMaxRetry;
        private Integer batchRequestRecoveryDelay; //controllare perchè doppia env
        private Integer batchRequestRecoveryLockAtMost;
        private Integer batchRequestRecoveryLockAtLeast;
        private Integer batchRequestRecoveryAfter;
        private Integer batchPollingRecoveryDelay; //controllare perchè doppia env
        private Integer batchPollingRecoveryLockAtMost;
        private Integer batchPollingRecoveryLockAtLeast;
        private Integer batchPollingRecoveryAfter;
        private Integer batchSqsRecoveryDelay; //controllare perchè doppia env
        private Integer batchSqsRecoveryLockAtMost;
        private Integer batchSqsRecoveryLockAtLeast;
        private Integer maxBatchRequestSize;

    }

    @Data
    public static class Inad {
        private String baseUrl;
        private String pdndClientSecret;
    }

    @Data
    public static class Ade {
        private String legalBaseUrl;
        private String auth;
        private String legalNameId;
        private String legalTrustSecret;
        private String checkCfBaseUrl;
        private String checkCfPdndClientSecret;
        private String checkCfTrustSecret;
    }

    @Data
    public static class Anpr {
        private String table;
        private String pdndClientSecret;
        private String trustSecret;
        private String baseUrl;
    }

    @Data
    public static class Pdnd {
        private String clientAssertionType;
        private String grantType;
        private Integer tokenDeadline;
        private String baseUrl;
    }

    @Data
    public static class Dao {
        private String shedlockTableName;
        private String batchPollingTableName;
        private String batchRequestTableName;
    }

}
