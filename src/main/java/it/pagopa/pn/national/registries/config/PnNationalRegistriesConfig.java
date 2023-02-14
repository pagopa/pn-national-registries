package it.pagopa.pn.national.registries.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Getter
@ToString
@Configuration
@ConfigurationProperties(prefix = "pn.national-registry")
@Import(SharedAutoConfiguration.class)
public class PnNationalRegistriesConfig {

    public static final String PDND_M2M_TOKEN = "pdnd";

    private String anprX509CertificateChain;
    private String anprJWTHeaderDigestKeystoreAlias;

}
