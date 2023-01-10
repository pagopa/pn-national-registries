package it.pagopa.pn.national.registries.exceptions;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;

public class PnNationalregistriesExceptionCodes extends PnExceptionsCodes {

    // raccolgo qui tutti i codici di errore di external registries
    public static final String ERROR_CODE_CLIENTASSERTION = "PN_NATIONAL_REGISTRIES_CLIENTASSERTION";
    public static final String ERROR_CODE_PDND_TOKEN = "PN_NATIONAL_REGISTRIES_PDNDTOKEN";
    public static final String ERROR_CODE_ADDRESS_ANPR = "PN_NATIONAL_REGISTRIES_ADDRESS_ANPR";
    public static final String ERROR_CODE_CHECK_CF = "PN_NATIONAL_REGISTRIES_CHECK_CF";
    public static final String ERROR_CODE_INAD = "PN_NATIONAL_REGISTRIES_INAD";
    public static final String ERROR_CODE_SECRET_MANAGER = "PN_NATIONAL_REGISTRIES";
    public static final String ERROR_CODE_INI_PEC = "PN_NATIONAL_REGISTRIES_INFOCAMERE_INI_PEC";

    public static final String ERROR_CODE_REGISTRO_IMPRESE = "PN_NATIONAL_REGISTRIES_INFOCAMERE_REGISTRO_IMPRESE";

    public static final String ERROR_CODE_LEGALE_RAPPRESENTANTE = "PN_NATIONAL_REGISTRIES_INFOCAMERE_LEGALE_RAPPRESENTANTE";

    public static final String ERROR_MESSAGE_CLIENTASSERTION = "Errore di generazione client_assertion";
    public static final String ERROR_MESSAGE_PDND_TOKEN = "Errore durante la generazione del token da PDND";
    public static final String ERROR_MESSAGE_ADDRESS_ANPR = "Errore durante la chiamata al servizio E002 dell'E-Service C001 di ANPR";
    public static final String ERROR_MESSAGE_CHECK_CF = "Errore durante la chiamata al servizio VerificaCodiceFiscale";
    public static final String ERROR_MESSAGE_INAD = "Errore durante la chiamata al servizio EstrazioniPuntualiApi";

    public static final String ERROR_MESSAGE_INI_PEC = "Errore durante la chiamata al servizio iniPEC di InfoCamere";
    public static final String ERROR_MESSAGE_SECRET_MANAGER = "Secret Value not found";

    public static final String ERROR_MESSAGE_REGISTRO_IMPRESE = "Errore durante la chiamata al servizio getRegistroImpreseLegalAddress di InfoCamere";
    public static final String ERROR_MESSAGE_LEGALE_RAPPRESENTANTE = "Errore durante la chiamata al servizio legaleRappresentante di InfoCamere";



}
