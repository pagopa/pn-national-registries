package it.pagopa.pn.national.registries.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConfigurationProperties(prefix = "pn.national-registry")
@Slf4j
@Data
@ToString
@Import(SharedAutoConfiguration.class)
public class PnExternalRegistriesConfig {

    public static final String PDND_M2M_TOKEN = "pdnd";

    private String anprX509CertificateChain;
    private String anprJWTHeaderDigestKeystoreAlias;

}
