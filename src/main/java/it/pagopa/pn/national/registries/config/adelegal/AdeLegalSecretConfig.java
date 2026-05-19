package it.pagopa.pn.national.registries.config.adelegal;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
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

    public AdeLegalSecretConfig(SsmParameterConsumerActivation ssmParameterConsumerActivation,
            PnNationalRegistriesSecretService pnNationalRegistriesSecretService,
            NationalRegistriesConfig nationalRegistriesConfig) {
        this.authChannelData = nationalRegistriesConfig.getAde().getAuth();
        this.ssmParameterConsumerActivation = ssmParameterConsumerActivation;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
        this.trustData = nationalRegistriesConfig.getAde().getLegalTrustSecret();
    }
}
