package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.AddressModeEnum;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo;
import it.pagopa.pn.national.registries.model.anpr.AddressColorEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class FullAnprAddressStrategy extends UtilsAnprAddressStrategy implements AnprAddressStrategy  {

    @Override
    public String createAddressDetail(TipoIndirizzo indirizzo) {
        StringBuilder sb = new StringBuilder();

        if (Objects.isNull(indirizzo.getNumeroCivico()) || Objects.isNull(indirizzo.getNumeroCivico().getCivicoInterno())) {
            return "";
        }
        var civicoInterno = indirizzo.getNumeroCivico().getCivicoInterno();

        appendIfFits(sb, Optional.ofNullable(indirizzo.getNumeroCivico().getColore())
                .map(AddressColorEnum::getCodeFromValue)
                .orElse(""));
        appendIfFits(sb, Optional.ofNullable(civicoInterno.getCorte()).map(elem -> " Corte " + elem).orElse(""));
        appendIfFits(sb, Optional.ofNullable(civicoInterno.getScala()).map(elem -> " Scala " + elem).orElse(""));
        appendIfFits(sb, Optional.ofNullable(civicoInterno.getScalaEsterna()).map(elem -> " Scala est. " + elem).orElse(""));

        if (!Objects.isNull(civicoInterno.getInterno1()) && !Objects.isNull(civicoInterno.getInterno2())) {
            appendIfFits(sb, Optional.ofNullable(civicoInterno.getInterno1()).map(elem -> " Primo interno " + elem).orElse(""));
            appendIfFits(sb, Optional.ofNullable(civicoInterno.getEspInterno1()).orElse(""));
            appendIfFits(sb, Optional.ofNullable(civicoInterno.getInterno2()).map(elem -> " Secondo interno " + elem).orElse(""));
            appendIfFits(sb, Optional.ofNullable(civicoInterno.getEspInterno2()).orElse(""));
        } else if (!Objects.isNull(civicoInterno.getInterno1())) {
            appendIfFits(sb, Optional.ofNullable(civicoInterno.getInterno1()).map(elem -> " Interno " + elem).orElse(""));
            appendIfFits(sb, Optional.ofNullable(civicoInterno.getEspInterno1()).orElse(""));
        } else if (!Objects.isNull(civicoInterno.getInterno2())) {
            appendIfFits(sb, Optional.ofNullable(civicoInterno.getInterno2()).map(elem -> " Interno " + elem).orElse(""));
            appendIfFits(sb, Optional.ofNullable(civicoInterno.getEspInterno2()).orElse(""));
        }

        appendIfFits(sb, Optional.ofNullable(civicoInterno.getIsolato()).map(elem -> " Isolato " + elem).orElse(""));
        return sb.toString().strip();
    }

    @Override
    public String getStrategyName() {
        return AddressModeEnum.FULL.name();
    }
}
