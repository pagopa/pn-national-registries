package it.pagopa.pn.national.registries.config.checkcf;

import it.pagopa.pn.national.registries.config.CommonWebClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pn.national.registries.webclient.ade-check-cf")
public class CheckCfWebClientConfig extends CommonWebClientConfig {

}
