package it.pagopa.pn.national.registries.config.inad;

import it.pagopa.pn.national.registries.config.PnNationlRegistriesSecretConfig;
import it.pagopa.pn.national.registries.model.SecretValue;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class InadSecretConfig extends PnNationlRegistriesSecretConfig {

    private final SecretManagerService secretManagerService;
    private final SecretValue inadSecretValue;

    public InadSecretConfig(SecretManagerService secretManagerService,
                            @Value("${pn.national.registries.pdnd.inad.purpose-id}") String purposeId) {
        super(secretManagerService);
        this.secretManagerService = secretManagerService;
        this.inadSecretValue = getSecretValue(purposeId);
    }
}
