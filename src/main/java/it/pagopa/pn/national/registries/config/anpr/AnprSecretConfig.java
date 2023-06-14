package it.pagopa.pn.national.registries.config.anpr;

import it.pagopa.pn.national.registries.config.PnNationalRegistriesSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class AnprSecretConfig extends PnNationalRegistriesSecretConfig {

    private final SecretManagerService secretManagerService;

    private final PdndSecretValue anprPdndSecretValue;

    public AnprSecretConfig(SecretManagerService secretManagerService,
                            @Value("${pn.national.registries.pdnd.anpr.purpose-id}") String purposeId,
                            @Value("${pn.national.registries.pdnd.anpr.secret}") String pdndSecret) {
        super(secretManagerService);
        this.secretManagerService = secretManagerService;
        this.anprPdndSecretValue = getPdndSecretValue(purposeId, pdndSecret);
    }
}
