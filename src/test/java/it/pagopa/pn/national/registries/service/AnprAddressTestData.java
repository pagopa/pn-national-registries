package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoCivicoInterno;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoNumeroCivico;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoToponimo;

/**
 * Builders di supporto per i test delle strategie di composizione indirizzo ANPR.
 */
final class AnprAddressTestData {

    private AnprAddressTestData() {
    }

    static TipoIndirizzo indirizzo(String specie, String denominazione, TipoNumeroCivico numeroCivico) {
        TipoToponimo toponimo = new TipoToponimo();
        toponimo.setSpecie(specie);
        toponimo.setDenominazioneToponimo(denominazione);
        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setToponimo(toponimo);
        indirizzo.setNumeroCivico(numeroCivico);
        return indirizzo;
    }

    static TipoNumeroCivico civico(String numero, String lettera) {
        TipoNumeroCivico civico = new TipoNumeroCivico();
        civico.setNumero(numero);
        civico.setLettera(lettera);
        return civico;
    }

    static TipoNumeroCivico civicoMetrico(String metrico, String numero) {
        TipoNumeroCivico civico = civico(numero, null);
        civico.setMetrico(metrico);
        return civico;
    }

    static TipoNumeroCivico civicoSnc(String progSnc, String numero) {
        TipoNumeroCivico civico = civico(numero, null);
        civico.setProgSNC(progSnc);
        return civico;
    }

    static TipoIndirizzo indirizzoConDettaglio(String colore, TipoCivicoInterno civicoInterno) {
        TipoNumeroCivico civico = new TipoNumeroCivico();
        civico.setColore(colore);
        civico.setCivicoInterno(civicoInterno);
        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setNumeroCivico(civico);
        return indirizzo;
    }
}

