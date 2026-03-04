package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.AddressModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class PartialAnprAddressStrategy implements AnprAddressStrategy {

    @Override
    public String createAddress(it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo indirizzo) {
        if (indirizzo.getToponimo() == null || indirizzo.getNumeroCivico() == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        var toponimo = indirizzo.getToponimo();
        var numeroCivico = indirizzo.getNumeroCivico();

        appendIfFits(sb, Optional.ofNullable(toponimo.getSpecie()).orElse(""));
        appendIfFits(sb, Optional.ofNullable(toponimo.getDenominazioneToponimo()).orElse(""));

        if (!Objects.isNull(numeroCivico.getMetrico()) && StringUtils.hasText(numeroCivico.getMetrico()) && Integer.parseInt(numeroCivico.getMetrico()) > 0) {
            appendIfFits(sb, "KM " + numeroCivico.getMetrico());
            return sb.toString();
        }

        if (!Objects.isNull(numeroCivico.getProgSNC()) && StringUtils.hasText(numeroCivico.getProgSNC()) && Integer.parseInt(numeroCivico.getProgSNC()) > 0) {
            String houseNumber = constructHouseNumber(
                    Optional.ofNullable(numeroCivico.getNumero()).orElse(""),
                    Optional.ofNullable(numeroCivico.getLettera()).orElse("")
            );
            appendIfFits(sb, houseNumber);
            appendIfFits(sb, "SNC");
            return sb.toString();
        }

        String houseNumber = constructHouseNumber(
                Optional.ofNullable(numeroCivico.getNumero()).orElse(""),
                Optional.ofNullable(numeroCivico.getLettera()).orElse("")
        );
        appendIfFits(sb, houseNumber);

        return sb.toString();
    }

    @Override
    public String getStrategyName() {
        return AddressModeEnum.METRICO_COLORE.name();
    }
}
