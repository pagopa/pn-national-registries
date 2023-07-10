package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
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
        return switch (message) {
            case CODICE_FISCALE_VALIDO_NON_UTILIZZABILE -> CheckTaxIdOKDto.ErrorCodeEnum.ERR01;
            case CODICE_FISCALE_NON_VALIDO_AGGIORNATO_IN_ALTRO -> CheckTaxIdOKDto.ErrorCodeEnum.ERR02;
            case CODICE_FISCALE_NON_VALIDO -> CheckTaxIdOKDto.ErrorCodeEnum.ERR03;
            default -> null;
        };
    }

    public ADELegalOKDto adELegalResponseToDto(CheckValidityRappresentanteResp checkValidityRappresentanteResp) {
        ADELegalOKDto adeLegalOKDto = new ADELegalOKDto();
        adeLegalOKDto.setResultCode(ADELegalOKDto.ResultCodeEnum.fromValue(checkValidityRappresentanteResp.codiceRitorno));
        adeLegalOKDto.setVerificationResult(checkValidityRappresentanteResp.valido);
        adeLegalOKDto.setResultDetail(ADELegalOKDto.ResultDetailEnum.fromValue(checkValidityRappresentanteResp.dettaglioEsito));

        return adeLegalOKDto;
    }

}
