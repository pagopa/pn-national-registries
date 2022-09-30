package it.pagopa.pn.national.registries.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_CODE_INAD;

public class InadException extends PnInternalException {

    public InadException(Throwable err){
        super("Errore durante la chiamata al servizio EstrazioniPuntualiApi",ERROR_CODE_INAD, err);
    }
}
