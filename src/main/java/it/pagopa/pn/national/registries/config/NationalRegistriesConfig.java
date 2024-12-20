package it.pagopa.pn.national.registries.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConfigurationProperties(prefix = "pn.national.registries")
@Data
@Import(SharedAutoConfiguration.class)
public class NationalRegistriesConfig {

    private String pfNewWorkflowStart;

    private String pfNewWorkflowStop;

}
