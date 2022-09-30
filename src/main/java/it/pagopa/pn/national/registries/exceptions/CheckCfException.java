package it.pagopa.pn.national.registries.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_CODE_CHECK_CF;
import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_CODE_PDND_TOKEN;

public class CheckCfException extends PnInternalException {

    public CheckCfException(Throwable err){
        super("Errore durante la chiamata al servizio VerificaCodiceFiscale",ERROR_CODE_CHECK_CF, err);
    }
}
