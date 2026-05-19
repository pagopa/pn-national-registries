package it.pagopa.pn.national.registries.config.adecheckcf;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class CheckCfSecretConfig {
    private final String pdndSecret;
    private final String trustData;
    private final String authChannelData;

    public CheckCfSecretConfig(NationalRegistriesConfig nationalRegistriesConfig) {
        this.pdndSecret = nationalRegistriesConfig.getAde().getCheckCfPdndClientSecret();
        this.trustData = nationalRegistriesConfig.getAde().getCheckCfTrustSecret();
        this.authChannelData = nationalRegistriesConfig.getAde().getAuth();
    }
}
