package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ResidentialAddressDto;
import it.pagopa.pn.national.registries.model.anpr.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AddressAnprConverter {

    public GetAddressANPROKDto convertToGetAddressANPROKDto(ResponseE002OKDto responseE002OKDto, String cf) {
        GetAddressANPROKDto response = new GetAddressANPROKDto();
        if (responseE002OKDto != null && responseE002OKDto.getTestataRisposta() != null) {
            response.setClientOperationId(responseE002OKDto.getTestataRisposta().getIdOperazioneClient());
        }
        if (responseE002OKDto != null && responseE002OKDto.getListaSoggetti() != null
                && responseE002OKDto.getListaSoggetti().getDatiSoggetto() != null) {
            for (SubjectsInstitutionDataDto item : responseE002OKDto.getListaSoggetti().getDatiSoggetto()) {
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

    public List<ResidentialAddressDto> convertResidence(List<ResidenceDto> residenza) {
        return residenza.stream()
                .map(this::convertResidence)
                .toList();
    }

    public ResidentialAddressDto convertResidence(ResidenceDto dto) {
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
        if(indirizzo.getNumeroCivico()!=null && indirizzo.getNumeroCivico().getCivicoInterno()!=null)
           innerDto.setAddressDetail(indirizzo.getNumeroCivico().getCivicoInterno().getScala());
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
        return toponimo.getDenominazione() + "," + toponimo.getNumeroCivico();
    }

    private String createAddressString(AddressDto indirizzo) {
        if (indirizzo.getToponimo() != null && indirizzo.getNumeroCivico() != null) {
            return indirizzo.getToponimo().getSpecie() + " " + indirizzo.getToponimo().getDenominazioneToponimo() + " "
                    + indirizzo.getNumeroCivico().getNumero() + indirizzo.getNumeroCivico().getLettera();
        } else {
            return "";
        }
    }
}
