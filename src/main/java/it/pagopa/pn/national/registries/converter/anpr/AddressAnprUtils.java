package it.pagopa.pn.national.registries.utils.anpr;

import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.dto.*;

import java.util.ArrayList;
import java.util.List;

public class AddressAnprUtils {

    private AddressAnprUtils() {
    }

    public static GetAddressANPROKDto mapToResponseOk(RispostaE002OKDto rispostaE002OKDto, String cf) {
        GetAddressANPROKDto response = new GetAddressANPROKDto();
        if (rispostaE002OKDto != null && rispostaE002OKDto.getListaSoggetti() != null
                && rispostaE002OKDto.getListaSoggetti().getDatiSoggetto() != null) {
            for (TipoDatiSoggettiEnteDto item : rispostaE002OKDto.getListaSoggetti().getDatiSoggetto()) {
                if (item.getGeneralita() != null && item.getGeneralita().getCodiceFiscale() != null
                        && item.getGeneralita().getCodiceFiscale().getCodFiscale() != null
                        && item.getGeneralita().getCodiceFiscale().getCodFiscale().equalsIgnoreCase(cf)
                        && item.getResidenza() != null) {
                    response.setResidentialAddresses(ConvertToGetAddressANPROKDto(item.getResidenza()));
                }
            }
        }
        return response;
    }

    private static List<GetAddressANPROKResidentialAddressesInnerDto> ConvertToGetAddressANPROKDto(List<TipoResidenzaDto> residenza) {
        List<GetAddressANPROKResidentialAddressesInnerDto> list = new ArrayList<>();
        for (TipoResidenzaDto dto : residenza) {
            GetAddressANPROKResidentialAddressesInnerDto innerDto = new GetAddressANPROKResidentialAddressesInnerDto();

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

    private static String createAddressString(TipoIndirizzoDto indirizzo) {
        //TODO: CAPIRE COME COSTRUIRE LA STRINGA CORRETTAMENTE
        if (indirizzo.getToponimo() != null && indirizzo.getNumeroCivico() != null) {
            return indirizzo.getToponimo().getSpecie() + " " + indirizzo.getToponimo().getDenominazioneToponimo() + " "
                    + indirizzo.getNumeroCivico().getNumero() + indirizzo.getNumeroCivico().getLettera();
        } else {
            return "";
        }
    }

    public static RichiestaE002Dto createRequest(GetAddressANPRRequestBodyDto request) {
        RichiestaE002Dto richiesta = new RichiestaE002Dto();
        richiesta.getCriteriRicerca().codiceFiscale(request.getFilter().getTaxId());
        return richiesta;
    }
}
