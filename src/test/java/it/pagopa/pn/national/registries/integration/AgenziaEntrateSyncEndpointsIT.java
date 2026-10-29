package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.exceptions.RuntimeJAXBException;
import it.pagopa.pn.national.registries.service.AgenziaEntrateService;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.dto.VerificaCodiceFiscale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Scenari funzionali: Copre gli endpoint sincroni di Agenzia Entrate per validazione del codice fiscale e verifica del rappresentante legale, inclusi whitelist, esito negativo ed errore XML.
 *
 * <p>Configurazione specifica della classe:</p>
 * <ul>
 *   <li>Nessun override locale: usa la configurazione comune definita in {@link AbstractNationalRegistriesComponentIT}.</li>
 * </ul>
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
class AgenziaEntrateSyncEndpointsIT extends AbstractNationalRegistriesComponentIT {

    @Autowired
    private AgenziaEntrateService agenziaEntrateService;

    /**
     * Verifica che un codice fiscale valido venga controllato tramite check CF e produca esito positivo.
     */
    @Test
    void AgenziaEntrateTaxId_ValidTaxIdUsesCheckCfAndReturnsTrue() {
        VerificaCodiceFiscale response = new VerificaCodiceFiscale();
        response.setCodiceFiscale(VALID_PF_CF);
        response.setValido(true);
        when(checkCfClient.callEService(any())).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/national-registries-private/agenzia-entrate/tax-id")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"filter\":{\"taxId\":\"" + VALID_PF_CF + "\"}}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.taxId").isEqualTo(VALID_PF_CF)
                .jsonPath("$.isValid").isEqualTo(true);

        verify(checkCfClient).callEService(any());
    }

    /**
     * Verifica che un codice fiscale inesistente produca esito negativo con errore correttamente mappato.
     */
    @Test
    void AgenziaEntrateTaxId_NonExistingTaxIdReturnsFalseAndMappedError() {
        VerificaCodiceFiscale response = new VerificaCodiceFiscale();
        response.setCodiceFiscale(VALID_PF_CF);
        response.setValido(false);
        response.setMessaggio("Codice fiscale non valido");
        when(checkCfClient.callEService(any())).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/national-registries-private/agenzia-entrate/tax-id")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"filter\":{\"taxId\":\"" + VALID_PF_CF + "\"}}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.taxId").isEqualTo(VALID_PF_CF)
                .jsonPath("$.isValid").isEqualTo(false)
                .jsonPath("$.errorCode").isEqualTo("B001_CHECK_TAX_ID_ERR03");
    }

    /**
     * Verifica l’esito positivo quando il rappresentante legale restituito coincide con quello richiesto.
     */
    @Test
    void AgenziaEntrateLegal_MatchingRepresentativeReturnsPositiveResult() {
        when(adELegalClient.checkTaxIdAndVatNumberAdE(any())).thenReturn(Mono.just(soapResponse(true, "00", "XX00")));

        webTestClient.post()
                .uri("/national-registries-private/agenzia-entrate/legal")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(legalRequest())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.verificationResult").isEqualTo(true)
                .jsonPath("$.resultCode").isEqualTo("00")
                .jsonPath("$.resultDetail").isEqualTo("XX00")
                .jsonPath("$.resultDetailMessage").isEqualTo("richiesta corretta");
    }

    /**
     * Verifica l’esito negativo quando il rappresentante legale restituito non coincide con quello richiesto.
     */
    @Test
    void AgenziaEntrateLegal_NonMatchingRepresentativeReturnsNegativeResult() {
        when(adELegalClient.checkTaxIdAndVatNumberAdE(any())).thenReturn(Mono.just(soapResponse(false, "00", "XX01")));

        webTestClient.post()
                .uri("/national-registries-private/agenzia-entrate/legal")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(legalRequest())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.verificationResult").isEqualTo(false)
                .jsonPath("$.resultCode").isEqualTo("00")
                .jsonPath("$.resultDetail").isEqualTo("XX01");
    }

    /**
     * Verifica la gestione di una risposta XML malformata del servizio di rappresentanza legale.
     */
    @Test
    void AgenziaEntrateLegal_MalformedXmlRaisesRuntimeJaxbException() {
        when(adELegalClient.checkTaxIdAndVatNumberAdE(any())).thenReturn(Mono.just("<not-valid-xml>"));

        assertThrows(RuntimeJAXBException.class, () -> agenziaEntrateService.checkTaxIdAndVatNumber(
                objectMapper.readValue(legalRequest(), it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalRequestBodyDto.class)
        ).block());
    }

    private String legalRequest() {
        return "{\"filter\":{\"taxId\":\"" + VALID_PF_CF + "\",\"vatNumber\":\"" + VALID_PG_PIVA + "\"}}";
    }

    private String soapResponse(boolean valid, String resultCode, String detail) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>"
                + "<ns2:checkValidityRappresentanteResp xmlns:ns2=\"http://anagrafica.verifica.rappresentante.ente\">"
                + "<ns2:valido>" + valid + "</ns2:valido>"
                + "<ns2:dettaglioEsito>" + detail + "</ns2:dettaglioEsito>"
                + "<ns2:codiceRitorno>" + resultCode + "</ns2:codiceRitorno>"
                + "</ns2:checkValidityRappresentanteResp></soapenv:Body></soapenv:Envelope>";
    }
}
