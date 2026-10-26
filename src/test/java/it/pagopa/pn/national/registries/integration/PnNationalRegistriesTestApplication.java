package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.PnNationalRegistriesApplication;
import it.pagopa.pn.national.registries.config.PnNationalRegistriesSchedulingConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = "it.pagopa.pn.national.registries",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PnNationalRegistriesApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PnNationalRegistriesSchedulingConfiguration.class)
        }
)
public class PnNationalRegistriesTestApplication {
}