package it.pagopa.pn.national.registries.config.inipec;

import it.pagopa.pn.national.registries.config.PnNationlRegistriesSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.model.SecretValue;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class IniPecSecretConfig extends PnNationlRegistriesSecretConfig {

    private final SecretManagerService secretManagerService;

    private final SSLData iniPecIntegritySecret;
    private final SecretValue iniPecSecretValue;

    public IniPecSecretConfig(SecretManagerService secretManagerService,
                              @Value("${pn.national.registries.pdnd.inipec.purpose-id}") String purposeId,
                              @Value("${pn.national.registries.pdnd.inipec.secret.integrity}") String integritySecret) {
        super(secretManagerService);
        this.secretManagerService = secretManagerService;
        this.iniPecIntegritySecret = getSslDataSecretValue(integritySecret);
        this.iniPecSecretValue = getSecretValue(purposeId);
    }
}
