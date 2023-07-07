package it.pagopa.pn.national.registries.config.adelegal;

import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class AdeLegalSecretConfig {
    private final String authChannelData;

    private final SsmParameterConsumerActivation ssmParameterConsumerActivation;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;
    private final String trustData;

    public AdeLegalSecretConfig(
            @Value("${pn.national.registries.ade.auth}") String authChannelData,
            SsmParameterConsumerActivation ssmParameterConsumerActivation,
            PnNationalRegistriesSecretService pnNationalRegistriesSecretService,
            @Value("${pn.national.registries.ade.legal.trust.secret}") String trustData) {
        this.authChannelData = authChannelData;
        this.ssmParameterConsumerActivation = ssmParameterConsumerActivation;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
        this.trustData = trustData;
    }
}
