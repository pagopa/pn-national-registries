package it.pagopa.pn.national.registries.config.ipa;

import it.pagopa.pn.national.registries.config.CommonWebClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pn.national.registries.webclient.ipa")
public class IpaWebClientConfig extends CommonWebClientConfig {

}
