package it.pagopa.pn.national.registries.config.anpr;

import it.pagopa.pn.national.registries.config.PnNationalRegistriesSecretConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.TrustData;
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
    private final TrustData trustData;

    public AnprSecretConfig(SecretManagerService secretManagerService,
                            @Value("${pn.national.registries.pdnd.anpr.purpose-id}") String purposeId,
                            @Value("${pn.national.registries.pdnd.anpr.secret}") String pdndSecret,
                            @Value("${pn.national.registries.trust.anpr.secret}") String trustData) {
        super(secretManagerService);
        this.secretManagerService = secretManagerService;
        this.trustData = getTrustedCertFromSecret(trustData);
        this.anprPdndSecretValue = getPdndSecretValue(purposeId, pdndSecret);
    }
}
