package it.pagopa.pn.national.registries.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_CODE_CLIENTASSERTION;

public class AssertionGeneratorException extends PnInternalException {

    public AssertionGeneratorException( Throwable err){
        super("Errore di generazione client_assertion", ERROR_CODE_CLIENTASSERTION, err);
    }
}


