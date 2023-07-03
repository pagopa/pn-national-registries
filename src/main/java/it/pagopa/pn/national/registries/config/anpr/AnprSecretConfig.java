package it.pagopa.pn.national.registries.config.anpr;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class AnprSecretConfig{

    private final String purposeId;
    private final String pdndSecretName;
    private final String environmentType;
    private final String trustSecret;

    public AnprSecretConfig(@Value("${pn.national.registries.trust.anpr.secret}") String trustSecret,
                            @Value("${pn.national.registries.pdnd.anpr.purpose-id}") String purposeId,
                            @Value("${pn.national.registries.pdnd.anpr.secret}") String pdndSecretName,
                            @Value("${pn.national.registries.environment.type}") String environmentType){
        this.purposeId = purposeId;
        this.pdndSecretName = pdndSecretName;
        this.environmentType = environmentType;
        this.trustSecret = trustSecret;
    }
}
