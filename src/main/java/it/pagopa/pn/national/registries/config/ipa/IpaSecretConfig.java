package it.pagopa.pn.national.registries.config.ipa;

import it.pagopa.pn.national.registries.config.PnNationalRegistriesSecretConfig;
import it.pagopa.pn.national.registries.model.ipa.IpaSecret;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class IpaSecretConfig extends PnNationalRegistriesSecretConfig {

    private final SecretManagerService secretManagerService;

    private final IpaSecret ipaSecret;

    public IpaSecretConfig(SecretManagerService secretManagerService,
                           @Value("${pn.national.registries.ipa.secret}") String ipaSecret) {
        super(secretManagerService);
        this.secretManagerService = secretManagerService;
        this.ipaSecret = getIpaSecret(ipaSecret);
    }
}
