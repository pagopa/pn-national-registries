package it.pagopa.pn.national.registries.config.adecheckcf;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class CheckCfSecretConfig{
    private final String pdndSecret;
    private final String trustData;

    public CheckCfSecretConfig(@Value("${pn.national.registries.pdnd.ade-check-cf.secret}") String pdndSecret,
                               @Value("${pn.national.registries.trust.ade-check-cf.secret}") String trustData) {
        this.pdndSecret = pdndSecret;
        this.trustData = trustData;
    }
}
