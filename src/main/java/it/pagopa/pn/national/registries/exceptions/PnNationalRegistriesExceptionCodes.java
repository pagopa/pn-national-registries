package it.pagopa.pn.national.registries.exceptions;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;

public class PnNationalRegistriesExceptionCodes extends PnExceptionsCodes {

    // codici di errore di national registries

    // PDND
    public static final String ERROR_CODE_CLIENTASSERTION = "PN_NATIONAL_REGISTRIES_CLIENTASSERTION";
    // ANPR
    public static final String ERROR_CODE_ANPR = "PN_NATIONAL_REGISTRIES_ADDRESS_ANPR";
    // ADE - CheckCF
    public static final String ERROR_CODE_CHECK_CF = "PN_NATIONAL_REGISTRIES_CHECK_CF";
    public static final String ERROR_CODE_ADE = "PN_NATIONAL_REGISTRIES_ADE";
    // INAD
    public static final String ERROR_CODE_INAD = "PN_NATIONAL_REGISTRIES_INAD";
    // INFOCAMERE
    public static final String ERROR_CODE_INFOCAMERE = "PN_NATIONAL_REGISTRIES_INFOCAMERE";
    public static final String ERROR_CODE_INIPEC = "PN_NATIONAL_REGISTRIES_INFOCAMERE_INI_PEC";
    public static final String ODE_REGISTRO_IMPRESE = "PN_NATIONAL_REGISTRIES_INFOCAMERE_REGISTRO_IMPRESE";
    public static final String ODE_LEGALE_RAPPRESENTANTE = "PN_NATIONAL_REGISTRIES_INFOCAMERE_LEGALE_RAPPRESENTANTE";
    // IPA
    public static final String ERROR_CODE_IPA = "PN_NATIONAL_REGISTRIES_IPA";

    // altri codici di errore...

    public static final String ERROR_CODE_SECRET_MANAGER = "PN_NATIONAL_REGISTRIES_SECRET_MANAGER";

    public static final String ERROR_CODE_UNAUTHORIZED = "PN_NATIONAL_REGISTRIES_UNAUTHORIZED";
    public static final String ERROR_CODE_INVALID_RECIPIENTTYPE = "PN_NATIONAL_REGISTRIES_INVALID_RECIPIENTTYPE";
    public static final String ERROR_CODE_INVALID_DOMICILETYPE = "PN_NATIONAL_REGISTRIES_INVALID_DOMICILETYPE";
    public static final String ERROR_CODE_INFOCAMERE_TOKEN_DURATION = "PN_NATIONAL_REGISTRIES_INFOCAMERE_TOKEN_DURATION";

    public static final String ERROR_CODE_ADE_LEGAL_OPENSAML_INIT = "PN_NATIONAL_REGISTRIES_ADE_LEGAL_OPENSAML_INIT";
    public static final String ERROR_CODE_ADE_LEGAL_LOAD_CERT = "PN_NATIONAL_REGISTRIES_ADE_LEGAL_LOAD_CERT";
    public static final String ERROR_CODE_ADE_LEGAL_LOAD_KEY = "PN_NATIONAL_REGISTRIES_ADE_LEGAL_LOAD_KEY";
    public static final String ERROR_CODE_ADE_LEGAL_CREATE_SOAP = "PN_NATIONAL_REGISTRIES_ADE_LEGAL_CREATE_SOAP";

    // messaggi di errore di national registries

    // PDND
    public static final String ERROR_MESSAGE_CLIENTASSERTION = "Errore di generazione client_assertion";
    // ANPR
    public static final String ERROR_MESSAGE_ANPR = "Errore durante la chiamata al servizio E002 dell'E-Service C001 di ANPR";
    // ADE - CheckCF
    public static final String ERROR_MESSAGE_CHECK_CF = "Errore durante la chiamata al servizio VerificaCodiceFiscale";
    // INAD
    public static final String ERROR_MESSAGE_INAD = "Errore durante la chiamata al servizio EstrazioniPuntualiApi";
    // INFOCAMERE
    public static final String ERROR_MESSAGE_INFOCAMERE = "Errore durante la chiamata al servizio di InfoCamere";
    public static final String ERROR_MESSAGE_INIPEC = "Errore durante la chiamata al servizio iniPEC di InfoCamere";
    public static final String ERROR_MESSAGE_INIPEC_RETRY_EXHAUSTED_TO_SQS = "Errore durante il recupero della PEC richiesta - numero massimo di tentativi esaurito!";
    public static final String ERROR_MESSAGE_REGISTRO_IMPRESE = "Errore durante la chiamata al servizio getRegistroImpreseLegalAddress di InfoCamere";
    public static final String ERROR_MESSAGE_LEGALE_RAPPRESENTANTE = "Errore durante la chiamata al servizio legaleRappresentante di InfoCamere";
    // IPA
    public static final String ERROR_MESSAGE_IPA = "Errore durante la chiamata al servizio pec di IPA";

    // altri messaggi di errore...

    public static final String ERROR_MESSAGE_SECRET_MANAGER = "Secret not found";
    public static final String ERROR_MESSAGE_SECRET_MANAGER_CONVERTER = "Errore durante la conversione del Secret";

    public static final String ERROR_MESSSAGE_PDND_UNAUTHORIZED = "Errore di autorizzazione su PDND";
    public static final String ERROR_MESSAGE_ANPR_UNAUTHORIZED = "Errore di autorizzazione su ANPR";
    public static final String ERROR_MESSAGE_INAD_UNAUTHORIZED = "Errore di autorizzazione su INAD";
    public static final String ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED = "Errore di autorizzazione su InfoCamere";
    public static final String ERROR_MESSAGE_ADE_UNAUTHORIZED = "Errore di autorizzazione su AdE";
    public static final String ERROR_MESSAGE_INFOCAMERE_TOKEN_DURATION = "Errore, il token è scaduto";
    public static final String ERROR_MESSAGE_ADE_LEGAL_OPENSAML_INIT = "Errore durante l'inizializzazione di OpenSAML";
    public static final String ERROR_MESSAGE_ADE_LEGAL_LOAD_CERT = "Errore durante il caricamento del certificato";
    public static final String ERROR_MESSAGE_ADE_LEGAL_LOAD_KEY = "Errore durante il caricamento della private key";
    public static final String ERROR_MESSAGE_ADE_LEGAL_CREATE_SOAP = "Errore durante la creazione dell'envelope SOAP";

    public static final String ERROR_CODE_NATIONAL_REGISTRIES_HANDLEEVENTFAILED = "PN_NATIONAL_REGISTRIES_HANDLEEVENTFAILED";

}
