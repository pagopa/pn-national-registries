package it.pagopa.pn.national.registries.converter;


import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.RispostaE002OK;
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
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class AnprConverter {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_LEN = 44;

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

    private void appendIfFits(StringBuilder sb, String value) {
        String token = Optional.ofNullable(value).map(String::strip).orElse("");
        if (token.isEmpty()) return;

        //Serve per calcolare lo spazio necessario ad aggiungere il token, considerando anche lo spazio se sb non è vuoto
        int extra = token.length() + (sb.isEmpty() ? 0 : 1);
        if (sb.length() + extra > AnprConverter.MAX_LEN) return;

        if (!sb.isEmpty()) sb.append(' ');
        sb.append(token);
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
        appendIfFits(sb, Optional.ofNullable(numeroCivico.getEsponente1()).orElse(""));

        return sb.toString();
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
