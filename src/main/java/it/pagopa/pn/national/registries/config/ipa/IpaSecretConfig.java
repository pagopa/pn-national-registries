package it.pagopa.pn.national.registries.config.ipa;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class IpaSecretConfig {

    private final String ipaSecret;

    public IpaSecretConfig(NationalRegistriesConfig nationalRegistriesConfig) {
        this.ipaSecret = nationalRegistriesConfig.getIpa().getSecret();
    }
}
