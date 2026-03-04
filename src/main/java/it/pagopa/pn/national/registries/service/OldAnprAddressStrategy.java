package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class OldAnprAddressStrategy implements AnprAddressStrategy {

    @Override
    public String createAddressDetail(TipoIndirizzo indirizzo) {
        if (Objects.isNull(indirizzo.getNumeroCivico()) || Objects.isNull(indirizzo.getNumeroCivico().getCivicoInterno())) {
            return "";
        }
        return Optional.ofNullable(indirizzo.getNumeroCivico().getCivicoInterno().getScala()).orElse("");
    }

    @Override
    public String getStrategyName() {
        return "OLD";
    }
}
