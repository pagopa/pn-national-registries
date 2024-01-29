package it.pagopa.pn.national.registries.config.inad;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class InadSecretConfig{

   private final String pdndSecret;

    public InadSecretConfig(@Value("${pn.national.registries.pdnd.inad.secret}") String pdndSecret) {
        this.pdndSecret = pdndSecret;
    }
}
