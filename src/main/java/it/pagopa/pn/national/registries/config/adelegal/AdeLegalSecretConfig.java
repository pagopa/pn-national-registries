package it.pagopa.pn.national.registries.config.adelegal;

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
public class AdeLegalSecretConfig extends PnNationalRegistriesSecretConfig {

    private final SecretManagerService secretManagerService;
    private final SSLData adeSecretConfig;

    public AdeLegalSecretConfig(SecretManagerService secretManagerService,
                                @Value("${pn.national.registries.ade-legal.secret}") String secretName){
        super(secretManagerService);
        this.secretManagerService = secretManagerService;
        this.adeSecretConfig = getSslDataSecretValue(secretName);
    }
}
