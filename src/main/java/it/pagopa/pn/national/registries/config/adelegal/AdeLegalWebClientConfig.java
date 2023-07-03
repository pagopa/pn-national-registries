package it.pagopa.pn.national.registries.config.adelegal;

import it.pagopa.pn.national.registries.config.CommonWebClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pn.national.registries.webclient.ade-legal")
public class AdeLegalWebClientConfig extends CommonWebClientConfig {

}
