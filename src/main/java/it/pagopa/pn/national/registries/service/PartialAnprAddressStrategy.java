package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo;
import it.pagopa.pn.national.registries.model.anpr.AddressColorEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class PartialAnprAddressStrategy implements AnprAddressStrategy {

    @Override
    public String createAddressDetail(TipoIndirizzo indirizzo) {
        StringBuilder sb = new StringBuilder();

        if (Objects.isNull(indirizzo.getNumeroCivico()) || Objects.isNull(indirizzo.getNumeroCivico().getCivicoInterno())) {
            return "";
        }

        appendIfFits(sb, Optional.ofNullable(indirizzo.getNumeroCivico().getColore())
                .map(AddressColorEnum::getCodeFromValue)
                .orElse(""));
        appendIfFits(sb, Optional.ofNullable(indirizzo.getNumeroCivico().getCivicoInterno().getScala()).map(elem -> " Scala " + elem).orElse(""));
        return sb.toString().strip();
    }

    @Override
    public String getStrategyName() {
        return "METRICO_COLORE";
    }
}
