package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.AddressModeEnum;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class OldAnprAddressStrategy extends UtilsAnprAddressStrategy implements AnprAddressStrategy {

    @Override
    public String createAddress(TipoIndirizzo indirizzo) {
        if (indirizzo.getToponimo() != null && indirizzo.getNumeroCivico() != null) {
            return Optional.ofNullable(indirizzo.getToponimo().getSpecie()).orElse("") + " "
                    + indirizzo.getToponimo().getDenominazioneToponimo() + " "
                    + constructHouseNumber(Optional.ofNullable(indirizzo.getNumeroCivico().getNumero()).orElse(""),
                    Optional.ofNullable(indirizzo.getNumeroCivico().getLettera()).orElse(""));
        } else {
            return "";
        }
    }

    @Override
    public String createAddressDetail(TipoIndirizzo indirizzo) {
        if (Objects.isNull(indirizzo.getNumeroCivico()) || Objects.isNull(indirizzo.getNumeroCivico().getCivicoInterno())) {
            return "";
        }
        return Optional.ofNullable(indirizzo.getNumeroCivico().getCivicoInterno().getScala()).orElse("");
    }

    @Override
    public String getStrategyName() {
        return AddressModeEnum.OLD.name();
    }
}
