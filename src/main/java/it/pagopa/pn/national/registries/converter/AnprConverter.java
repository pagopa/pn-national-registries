package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ResidentialAddressDto;
import it.pagopa.pn.national.registries.model.anpr.*;
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

    public GetAddressANPROKDto convertToGetAddressANPROKDto(ResponseE002OKDto responseE002OKDto, String cf) {
        GetAddressANPROKDto response = new GetAddressANPROKDto();
        if (responseE002OKDto != null) {
            response.setClientOperationId(responseE002OKDto.getIdOperazioneANPR());
        }
        if (responseE002OKDto != null && responseE002OKDto.getListaSoggetti() != null
                && responseE002OKDto.getListaSoggetti().getDatiSoggetto() != null) {
            response.setResidentialAddresses(responseE002OKDto.getListaSoggetti().getDatiSoggetto().stream()
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

    private ResidentialAddressDto convertResidence(ResidenceDto dto) {
        ResidentialAddressDto innerDto = new ResidentialAddressDto();
        innerDto.setAt(dto.getPresso());
        innerDto.setDescription(dto.getTipoIndirizzo());
        if (dto.getIndirizzo() != null) {
            mapToResidence(dto.getIndirizzo(), innerDto);
            if (dto.getLocalitaEstera() != null && dto.getLocalitaEstera().getIndirizzoEstero() != null
                    && dto.getLocalitaEstera().getIndirizzoEstero().getLocalita() != null) {
                innerDto.setForeignState(dto.getLocalitaEstera().getIndirizzoEstero().getLocalita().getDescrizioneStato());
            }

        } else if (dto.getLocalitaEstera() != null) {
            mapToForeignResidence(dto.getLocalitaEstera(), innerDto);
        }
        return innerDto;
    }

    private void mapToResidence(AddressDto indirizzo, ResidentialAddressDto innerDto) {
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

    private void mapToForeignResidence(ForeignLocation1Dto localitaEstera, ResidentialAddressDto innerDto) {
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

    private String createForeignAddressString(ForeignToponymDto toponimo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(toponimo.getDenominazione());
        if(StringUtils.hasText(toponimo.getNumeroCivico())){
            stringBuilder.append(" ");
            stringBuilder.append(toponimo.getNumeroCivico());
        }
        return stringBuilder.toString();
    }

    private String createAddressString(AddressDto indirizzo) {
        if (indirizzo.getToponimo() != null && indirizzo.getNumeroCivico() != null) {
            return indirizzo.getToponimo().getSpecie() + " " + indirizzo.getToponimo().getDenominazioneToponimo() + " "
                    + Optional.ofNullable(indirizzo.getNumeroCivico().getNumero()).orElse("") + Optional.ofNullable(indirizzo.getNumeroCivico().getLettera()).orElse("");
        } else {
            return "";
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
