package it.pagopa.pn.national.registries.config.infocamere;

import it.pagopa.pn.national.registries.config.PnNationalRegistriesSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class InfoCamereSecretConfig extends PnNationalRegistriesSecretConfig {

    private final SecretManagerService secretManagerService;

    private final SSLData iniPecAuthRestSecret;

    public InfoCamereSecretConfig(SecretManagerService secretManagerService,
                                  @Value("${pn.national.registries.infocamere.secret.auth-rest}") String iniPecAuthRestSecret) {
        super(secretManagerService);
        this.secretManagerService = secretManagerService;
        this.iniPecAuthRestSecret = getSslDataSecretValue(iniPecAuthRestSecret);
    }
}
