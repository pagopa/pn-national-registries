package it.pagopa.pn.national.registries.converter;


import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ResidentialAddressDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class AnprConverter {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public GetAddressANPROKDto convertToGetAddressANPROK(RispostaE002OK rispostaE002OK, String cf) {
        GetAddressANPROKDto response = new GetAddressANPROKDto();
        if (rispostaE002OK != null) {
            response.setClientOperationId(rispostaE002OK.getIdOperazioneANPR());
        }
        if (rispostaE002OK != null && rispostaE002OK.getListaSoggetti() != null
                && rispostaE002OK.getListaSoggetti().getDatiSoggetto() != null) {
            response.setResidentialAddresses(rispostaE002OK.getListaSoggetti().getDatiSoggetto().stream()
                    .filter(soggetto -> soggetto.getResidenza() != null
                            && soggetto.getGeneralita() != null
                            && soggetto.getGeneralita().getCodiceFiscale() != null
                            && soggetto.getGeneralita().getCodiceFiscale().getCodFiscale() != null
                            && soggetto.getGeneralita().getCodiceFiscale().getCodFiscale().equalsIgnoreCase(cf))
                    .flatMap(soggetto -> soggetto.getResidenza().stream())
                    .max(Comparator.comparing(r -> parseStringToDate(r.getDataDecorrenzaResidenza())))
                    .map(this::convertResidence)
                    .map(List::of)
                    .orElse(null));
        }
        return response;
    }

    private ResidentialAddressDto convertResidence(it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoResidenza tipoResidenza) {
        ResidentialAddressDto innerDto = new ResidentialAddressDto();
        innerDto.setAt(tipoResidenza.getPresso());
        innerDto.setDescription(tipoResidenza.getTipoIndirizzo());
        if (tipoResidenza.getIndirizzo() != null) {
            mapToResidence(tipoResidenza.getIndirizzo(), innerDto);
            if (tipoResidenza.getLocalitaEstera() != null && tipoResidenza.getLocalitaEstera().getIndirizzoEstero() != null
                    && tipoResidenza.getLocalitaEstera().getIndirizzoEstero().getLocalita() != null) {
                innerDto.setForeignState(tipoResidenza.getLocalitaEstera().getIndirizzoEstero().getLocalita().getDescrizioneStato());
            }

        } else if (tipoResidenza.getLocalitaEstera() != null) {
            mapToForeignResidence(tipoResidenza.getLocalitaEstera(), innerDto);
        }
        return innerDto;
    }

    private void mapToResidence(it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo indirizzo, ResidentialAddressDto innerDto) {
        if(indirizzo.getNumeroCivico()!=null && indirizzo.getNumeroCivico().getCivicoInterno()!=null){
            innerDto.setAddressDetail(indirizzo.getNumeroCivico().getCivicoInterno().getScala());
        }
        innerDto.setAddress(createAddressString(indirizzo));
        innerDto.setZip(indirizzo.getCap());
        innerDto.setMunicipalityDetails(indirizzo.getFrazione());

        if (indirizzo.getComune() != null) {
            innerDto.setMunicipality(indirizzo.getComune().getNomeComune());
            innerDto.setProvince(indirizzo.getComune().getSiglaProvinciaIstat());
        }
    }

    private void mapToForeignResidence(it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoLocalitaEstera1 localitaEstera, ResidentialAddressDto innerDto) {
        if (localitaEstera.getIndirizzoEstero() != null) {
            innerDto.setZip(localitaEstera.getIndirizzoEstero().getCap());
            if (localitaEstera.getIndirizzoEstero().getToponimo() != null) {
                innerDto.setAddress(createForeignAddressString(localitaEstera.getIndirizzoEstero().getToponimo()));
            }
        }
        if (localitaEstera.getIndirizzoEstero() != null
                && localitaEstera.getIndirizzoEstero().getLocalita() != null) {
            innerDto.setForeignState(localitaEstera.getIndirizzoEstero().getLocalita().getDescrizioneStato());
            innerDto.setMunicipality(localitaEstera.getIndirizzoEstero().getLocalita().getDescrizioneLocalita());
            innerDto.setProvince(localitaEstera.getIndirizzoEstero().getLocalita().getProvinciaContea());
        }
    }

    private String createForeignAddressString(it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoToponimoEstero toponimo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(toponimo.getDenominazione());
        if(StringUtils.hasText(toponimo.getNumeroCivico())){
            stringBuilder.append(" ");
            stringBuilder.append(toponimo.getNumeroCivico());
        }
        return stringBuilder.toString();
    }

    private String createAddressString(it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo indirizzo) {
        if (indirizzo.getToponimo() != null && indirizzo.getNumeroCivico() != null) {
            return Optional.ofNullable(indirizzo.getToponimo().getSpecie()).orElse("") + " "
                    + indirizzo.getToponimo().getDenominazioneToponimo() + " "
                    + constructHouseNumber(Optional.ofNullable(indirizzo.getNumeroCivico().getNumero()).orElse(""),
                    Optional.ofNullable(indirizzo.getNumeroCivico().getLettera()).orElse(""));
        } else {
            return "";
        }
    }

    private String constructHouseNumber(String numeroCivico, String letteraNumeroCivico) {
        if (StringUtils.hasText(numeroCivico) && StringUtils.hasText(letteraNumeroCivico)) {
            return numeroCivico + "/" + letteraNumeroCivico;
        }else {
            return numeroCivico + letteraNumeroCivico;
        }
    }

    private LocalDate parseStringToDate(String str) {
        if (str == null) {
            log.warn("can not parse a null date");
            return LocalDate.EPOCH;
        }
        try {
            return LocalDate.parse(str, formatter);
        } catch (DateTimeParseException e) {
            log.warn("can not parse date {}", str, e);
            return LocalDate.EPOCH;
        }
    }
}
