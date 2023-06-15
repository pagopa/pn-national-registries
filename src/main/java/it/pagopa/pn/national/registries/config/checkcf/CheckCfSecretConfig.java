package it.pagopa.pn.national.registries.config.checkcf;

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
public class CheckCfSecretConfig extends PnNationalRegistriesSecretConfig {

    private final SecretManagerService secretManagerService;
    private final PdndSecretValue checkCfPdndSecretValue;
    private final TrustData trustData;

    public CheckCfSecretConfig(SecretManagerService secretManagerService,
                               @Value("${pn.national.registries.pdnd.ade-check-cf.purpose-id}") String purposeId,
                               @Value("${pn.national.registries.pdnd.ade-check-cf.secret}") String pdndSecret,
                               @Value("${pn.national.registries.trust.ade-check-cf.secret}") String trustData) {
        super(secretManagerService);
        this.secretManagerService = secretManagerService;
        this.trustData = getTrustedCertFromSecret(trustData);
        this.checkCfPdndSecretValue = getPdndSecretValue(purposeId, pdndSecret);
    }
}
