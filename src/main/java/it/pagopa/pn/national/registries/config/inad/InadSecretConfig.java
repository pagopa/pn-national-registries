package it.pagopa.pn.national.registries.config.inad;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class InadSecretConfig{

   private final String pdndSecret;

    public InadSecretConfig(NationalRegistriesConfig nationalRegistriesConfig) {
        this.pdndSecret = nationalRegistriesConfig.getInad().getPdndClientSecret();
    }
}
