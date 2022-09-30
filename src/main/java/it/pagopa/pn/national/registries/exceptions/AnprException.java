package it.pagopa.pn.national.registries.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_CODE_ADDRESS_ANPR;

public class AnprException extends PnInternalException {

    public AnprException(Throwable err){
        super("Errore durante la chiamata al servizio E002 dell'E-Service C001 di ANPR", ERROR_CODE_ADDRESS_ANPR, err);
    }
}
