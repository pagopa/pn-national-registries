package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.AddressModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class FullAnprAddressStrategy extends UtilsAnprAddressStrategy implements AnprAddressStrategy  {

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

        if (!Objects.isNull(numeroCivico.getMetrico()) && StringUtils.hasText(numeroCivico.getMetrico()) && NumberUtils.isDigits(numeroCivico.getMetrico()) && Integer.parseInt(numeroCivico.getMetrico()) > 0) {
            appendIfFits(sb, "KM " + numeroCivico.getMetrico());
            return sb.toString();
        }

        if (!Objects.isNull(numeroCivico.getProgSNC()) && StringUtils.hasText(numeroCivico.getProgSNC()) && NumberUtils.isDigits(numeroCivico.getProgSNC()) && Integer.parseInt(numeroCivico.getProgSNC()) > 0) {
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
        appendIfFits(sb, Optional.ofNullable(numeroCivico.getEsponente1()).orElse(""));

        return sb.toString();
    }

    @Override
    public String getStrategyName() {
        return AddressModeEnum.FULL.name();
    }
}
