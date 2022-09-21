package it.pagopa.pn.national.registries.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.*;

public class PdndTokenGeneratorException extends PnInternalException {

    public PdndTokenGeneratorException(Throwable err){
        super("Errore durante la generazione del token da PDND", ERROR_CODE_CHECK_CF, err);
    }
}


