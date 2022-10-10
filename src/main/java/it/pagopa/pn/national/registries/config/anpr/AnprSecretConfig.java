package it.pagopa.pn.national.registries.config.anpr;

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
public class AnprSecretConfig extends PnNationlRegistriesSecretConfig {

    private final SecretManagerService secretManagerService;

    private final SSLData anprAuthChannelSecret;
    private final SSLData anprIntegritySecret;
    private final SecretValue anprSecretValue;

    public AnprSecretConfig(SecretManagerService secretManagerService,
                            @Value("${pn.national.registries.pdnd.anpr.purpose-id}") String purposeId,
                            @Value("${pn.national.registries.pdnd.anpr.secret.integrity}") String integritySecret,
                            @Value("${pn.national.registries.pdnd.anpr.secret.auth-channel}") String authChannelSecret) {
        super(secretManagerService);
        this.secretManagerService = secretManagerService;
        this.anprAuthChannelSecret = getSslDataSecretValue(authChannelSecret);
        this.anprIntegritySecret = getSslDataSecretValue(integritySecret);
        this.anprSecretValue = getSecretValue(purposeId);
    }
}
