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

    private final SSLData anprAuthChannelSecret;
    private final SSLData anprIntegritySecret;
    private final PdndSecretValue anprPdndSecretValue;

    public AnprSecretConfig(SecretManagerService secretManagerService,
                            @Value("${pn.national.registries.pdnd.anpr.purpose-id}") String purposeId,
                            @Value("${pn.national.registries.pdnd.anpr.secret}") String pdndSecret,
                            @Value("${pn.national.registries.anpr.secret.integrity}") String integritySecret,
                            @Value("${pn.national.registries.anpr.secret.auth-channel}") String authChannelSecret) {
        super(secretManagerService);
        this.secretManagerService = secretManagerService;
        this.anprAuthChannelSecret = getSslDataSecretValue(authChannelSecret);
        this.anprIntegritySecret = getSslDataSecretValue(integritySecret);
        this.anprPdndSecretValue = getPdndSecretValue(purposeId, pdndSecret);
    }
}
