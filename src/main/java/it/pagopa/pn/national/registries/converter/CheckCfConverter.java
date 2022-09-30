package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.dto.VerificaCodiceFiscale;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdOKDto;
import org.springframework.stereotype.Component;

@Component
public class CheckCfConverter {

    public static final String CODICE_FISCALE_VALIDO_NON_UTILIZZABILE = "Codice fiscale valido, non più utilizzabile in quanto aggiornato in altro codice fiscale";
    public static final String CODICE_FISCALE_NON_VALIDO_AGGIORNATO_IN_ALTRO = "Codice fiscale non utilizzabile in quanto aggiornato in altro codice fiscale";
    public static final String CODICE_FISCALE_NON_VALIDO = "Codice fiscale non valido";

    public CheckTaxIdOKDto convertToCfStatusDto(VerificaCodiceFiscale verificaCodiceFiscale) {
        CheckTaxIdOKDto cfStatusDto = new CheckTaxIdOKDto();
        cfStatusDto.setIsValid(verificaCodiceFiscale.getValido());
        cfStatusDto.setTaxId(verificaCodiceFiscale.getCodiceFiscale());
        if (verificaCodiceFiscale.getMessaggio() != null) {
            cfStatusDto.setErrorCode(decodeError(verificaCodiceFiscale.getMessaggio()));
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
}
