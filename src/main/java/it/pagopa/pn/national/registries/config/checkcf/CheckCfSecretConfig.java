package it.pagopa.pn.national.registries.config.checkcf;

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
public class CheckCfSecretConfig extends PnNationlRegistriesSecretConfig {

    private final SecretManagerService secretManagerService;
    private final SSLData checkCfAuthChannelSecret;
    private final SecretValue checkCfSecretValue;

    public CheckCfSecretConfig(SecretManagerService secretManagerService,
                               @Value("${pn.national.registries.pdnd.agenzia-entrate.purpose-id}") String purposeId,
                               @Value("${pn.national.registries.pdnd.check-cf.secret.auth-channel}") String authChannelSecret) {
        super(secretManagerService);
        this.secretManagerService = secretManagerService;
        this.checkCfAuthChannelSecret = getSslDataSecretValue(authChannelSecret);
        this.checkCfSecretValue = getSecretValue(purposeId);
    }
}
