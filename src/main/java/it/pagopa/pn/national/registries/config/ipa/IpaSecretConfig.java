package it.pagopa.pn.national.registries.config.ipa;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class IpaSecretConfig {


    private final String ipaSecret;

    public IpaSecretConfig(@Value("${pn.national.registries.ipa.secret}") String ipaSecret) {
        this.ipaSecret = ipaSecret;
    }
}
