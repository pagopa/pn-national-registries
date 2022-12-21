package it.pagopa.pn.national.registries.converter;

import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteRespType;
import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteType;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ADELegalOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ADELegalRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.TaxIdVerification;
import org.springframework.stereotype.Component;

@Component
public class AgenziaEntrateConverter {

    public static final String CODICE_FISCALE_VALIDO_NON_UTILIZZABILE = "Codice fiscale valido, non più utilizzabile in quanto aggiornato in altro codice fiscale";
    public static final String CODICE_FISCALE_NON_VALIDO_AGGIORNATO_IN_ALTRO = "Codice fiscale non utilizzabile in quanto aggiornato in altro codice fiscale";
    public static final String CODICE_FISCALE_NON_VALIDO = "Codice fiscale non valido";

    public CheckTaxIdOKDto convertToCfStatusDto(TaxIdVerification taxIdVerification) {
        CheckTaxIdOKDto cfStatusDto = new CheckTaxIdOKDto();
        cfStatusDto.setIsValid(taxIdVerification.getValido());
        cfStatusDto.setTaxId(taxIdVerification.getCodiceFiscale());
        if (taxIdVerification.getMessaggio() != null) {
            cfStatusDto.setErrorCode(decodeError(taxIdVerification.getMessaggio()));
        }
        return cfStatusDto;
    }

    public CheckTaxIdOKDto.ErrorCodeEnum decodeError(String message) {
        switch (message) {
            case CODICE_FISCALE_VALIDO_NON_UTILIZZABILE:
                return CheckTaxIdOKDto.ErrorCodeEnum.ERR01;
            case CODICE_FISCALE_NON_VALIDO_AGGIORNATO_IN_ALTRO:
                return CheckTaxIdOKDto.ErrorCodeEnum.ERR02;
            case CODICE_FISCALE_NON_VALIDO:
                return CheckTaxIdOKDto.ErrorCodeEnum.ERR03;
            default:
                return null;
        }
    }

    public ADELegalOKDto adELegalResponseToDto(CheckValidityRappresentanteRespType checkValidityRappresentanteRespType) {
        ADELegalOKDto adeLegalOKDto = new ADELegalOKDto();
        adeLegalOKDto.setResultCode(ADELegalOKDto.ResultCodeEnum.fromValue(checkValidityRappresentanteRespType.getCodiceRitorno()));
        adeLegalOKDto.setVerificationResult(checkValidityRappresentanteRespType.isValido());
        adeLegalOKDto.setResultDetail(ADELegalOKDto.ResultDetailEnum.fromValue(checkValidityRappresentanteRespType.getDettaglioEsito()));


        return adeLegalOKDto;
    }

    public CheckValidityRappresentanteType toEnvelopeBody(ADELegalRequestBodyFilterDto filter) {
        CheckValidityRappresentanteType checkValidityRappresentanteType = new CheckValidityRappresentanteType();
        checkValidityRappresentanteType.setCfRappresentante(filter.getTaxId());
        checkValidityRappresentanteType.setCfEnte(filter.getVatNumber());


        return checkValidityRappresentanteType;
    }
}
