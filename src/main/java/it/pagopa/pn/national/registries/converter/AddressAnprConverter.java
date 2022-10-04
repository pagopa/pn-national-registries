package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ResidentialAddressDto;
import it.pagopa.pn.national.registries.model.anpr.RispostaE002OKDto;
import it.pagopa.pn.national.registries.model.anpr.TipoDatiSoggettiEnteDto;
import it.pagopa.pn.national.registries.model.anpr.TipoIndirizzoDto;
import it.pagopa.pn.national.registries.model.anpr.TipoResidenzaDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AddressAnprConverter {

    public GetAddressANPROKDto convertToGetAddressANPROKDto(RispostaE002OKDto rispostaE002OKDto, String cf) {
        GetAddressANPROKDto response = new GetAddressANPROKDto();
        if (rispostaE002OKDto != null && rispostaE002OKDto.getListaSoggetti() != null
                && rispostaE002OKDto.getListaSoggetti().getDatiSoggetto() != null) {
            for (TipoDatiSoggettiEnteDto item : rispostaE002OKDto.getListaSoggetti().getDatiSoggetto()) {
                if (item.getGeneralita() != null && item.getGeneralita().getCodiceFiscale() != null
                        && item.getGeneralita().getCodiceFiscale().getCodFiscale() != null
                        && item.getGeneralita().getCodiceFiscale().getCodFiscale().equalsIgnoreCase(cf)
                        && item.getResidenza() != null) {
                    response.setResidentialAddresses(convertResidence(item.getResidenza()));
                }
            }
        }
        return response;
    }

    private List<ResidentialAddressDto> convertResidence(List<TipoResidenzaDto> residenza) {
        List<ResidentialAddressDto> list = new ArrayList<>();
        for (TipoResidenzaDto dto : residenza) {

            ResidentialAddressDto innerDto = new ResidentialAddressDto();

            if (dto.getIndirizzo() != null) {
                innerDto.setAddress(createAddressString(dto.getIndirizzo()));
                innerDto.setZip(dto.getIndirizzo().getCap());
                innerDto.setMunicipalityDetails(dto.getIndirizzo().getFrazione());
            }

            if (dto.getIndirizzo() != null && dto.getIndirizzo().getComune() != null) {
                innerDto.setMunicipality(dto.getIndirizzo().getComune().getNomeComune());
                innerDto.setProvince(dto.getIndirizzo().getComune().getSiglaProvinciaIstat());
            }

            if (dto.getLocalitaEstera() != null && dto.getLocalitaEstera().getIndirizzoEstero() != null
                    && dto.getLocalitaEstera().getIndirizzoEstero().getLocalita() != null) {
                innerDto.setForeignState(dto.getLocalitaEstera().getIndirizzoEstero().getLocalita().getDescrizioneLocalita());
            }

            innerDto.setAt(dto.getPresso());
            innerDto.setDescription(dto.getTipoIndirizzo());

            list.add(innerDto);
        }
        return list;
    }

    private String createAddressString(TipoIndirizzoDto indirizzo) {
        if (indirizzo.getToponimo() != null && indirizzo.getNumeroCivico() != null) {
            return indirizzo.getToponimo().getSpecie() + " " + indirizzo.getToponimo().getDenominazioneToponimo() + " "
                    + indirizzo.getNumeroCivico().getNumero() + indirizzo.getNumeroCivico().getLettera();
        } else {
            return "";
        }
    }
}
