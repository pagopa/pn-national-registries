package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.AddressModeEnum;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo;
import it.pagopa.pn.national.registries.model.anpr.AddressColorEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class MinimalAnprAddressStrategy extends UtilsAnprAddressStrategy implements AnprAddressStrategy {

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

            if ((!Objects.isNull(numeroCivico.getNumero()) && StringUtils.hasText(numeroCivico.getNumero()))) {
                String houseNumber = constructHouseNumber(
                        Optional.ofNullable(numeroCivico.getNumero()).orElse(""),
                        Optional.ofNullable(numeroCivico.getLettera()).orElse("")
                );
                appendIfFits(sb, houseNumber);
                log.error("Metric with civic number is not supported, metric: {}, civic number: {}, letter: {}, progSNC: {}, esponente1: {}",
                        indirizzo.getNumeroCivico().getMetrico(), indirizzo.getNumeroCivico().getNumero(),
                        indirizzo.getNumeroCivico().getLettera(), indirizzo.getNumeroCivico().getProgSNC(),
                        indirizzo.getNumeroCivico().getEsponente1());
            }

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

        return sb.toString();
    }

    @Override
    public String createAddressDetail(TipoIndirizzo indirizzo) {
        StringBuilder sb = new StringBuilder();

        if (Objects.isNull(indirizzo.getNumeroCivico())) {
            return "";
        }

        if(!Objects.isNull(indirizzo.getNumeroCivico().getColore())) {
            appendIfFits(sb, Optional.ofNullable(indirizzo.getNumeroCivico().getColore())
                    .map(AddressColorEnum::getCodeFromValue)
                    .orElse(""));
        }
        if(!Objects.isNull(indirizzo.getNumeroCivico().getCivicoInterno())) {
            appendIfFits(sb, Optional.ofNullable(indirizzo.getNumeroCivico().getCivicoInterno().getScala()).filter(StringUtils::hasText).map(elem -> " Scala " + elem).orElse(""));
        }
        return sb.toString().strip();
    }

    @Override
    public String getStrategyName() {
        return AddressModeEnum.MINIMAL.name();
    }
}
