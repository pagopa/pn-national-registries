package it.pagopa.pn.national.registries.config.inad;

import it.pagopa.pn.national.registries.config.PnNationalRegistriesSecretConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class InadSecretConfig extends PnNationalRegistriesSecretConfig {

    private final SecretManagerService secretManagerService;
    private final PdndSecretValue inadPdndSecretValue;

    public InadSecretConfig(SecretManagerService secretManagerService,
                            @Value("${pn.national.registries.pdnd.inad.purpose-id}") String purposeId,
                            @Value("${pn.national.registries.pdnd.inad.secret}") String pdndSecret) {
        super(secretManagerService);
        this.secretManagerService = secretManagerService;
        this.inadPdndSecretValue = getPdndSecretValue(purposeId, pdndSecret);
    }
}
