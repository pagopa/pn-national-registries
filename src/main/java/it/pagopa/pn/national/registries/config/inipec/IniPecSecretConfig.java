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
    private final SecretValue iniPecSecretValue;

    private final SSLData iniPecAuthRestSecret;
    private final SSLData iniPecIntegritySecret;

    public IniPecSecretConfig(SecretManagerService secretManagerService,
                              @Value("${pn.national.registries.pdnd.inipec.purpose-id}") String purposeId,
                              @Value("${pn.national.registries.pdnd.inipec.secret.integrity}") String iniPecIntegritySecret,
                              @Value("${pn.national.registries.pdnd.inipec.secret.auth-channel}") String iniPecAuthRestSecret) {
        super(secretManagerService);
        this.secretManagerService = secretManagerService;
        this.iniPecSecretValue = getSecretValue(purposeId);
        this.iniPecIntegritySecret = getSslDataSecretValue(iniPecIntegritySecret);
        this.iniPecAuthRestSecret = getSslDataSecretValue(iniPecAuthRestSecret);
    }
}
