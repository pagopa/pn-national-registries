package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.AddressRegistroImprese;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.InfoCamereLegalInstituionsResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.InfoCamereVerification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Scenari funzionali: Copre gli endpoint sincroni InfoCamere/Registro Imprese per indirizzo legale, rappresentante legale e imprese rappresentate, inclusi errori applicativi e risultati vuoti.
 *
 * <p>Configurazione specifica della classe:</p>
 * <ul>
 *   <li>Nessun override locale: usa la configurazione comune definita in {@link AbstractNationalRegistriesComponentIT}.</li>
 * </ul>
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
class InfoCamereSyncEndpointsIT extends AbstractNationalRegistriesComponentIT {

    /**
     * Verifica che l’endpoint Registro Imprese restituisca correttamente la sede legale.
     */
    @Test
    void RegistroImprese_AddressReturnsLegalAddress() {
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.just(
                registroImpreseAddress(VALID_PG_PIVA, "VIA", "ROMA", "10", "Roma", "RM", "00100", "IT")
        ));

        webTestClient.post()
                .uri("/national-registries-private/registro-imprese/address")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"filter\":{\"taxId\":\"" + VALID_PG_PIVA + "\"}}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.taxId").isEqualTo(VALID_PG_PIVA)
                .jsonPath("$.professionalAddress.address").isEqualTo("VIA ROMA 10")
                .jsonPath("$.professionalAddress.municipality").isEqualTo("Roma")
                .jsonPath("$.professionalAddress.province").isEqualTo("RM")
                .jsonPath("$.professionalAddress.zip").isEqualTo("00100")
                .jsonPath("$.professionalAddress.stato").isEqualTo("IT");
    }

    /**
     * Verifica che un errore applicativo Registro Imprese restituisca la richiesta senza indirizzo fisico.
     */
    @Test
    void RegistroImprese_ApplicationErrorReturnsRequestWithoutAddress() {
        AddressRegistroImprese response = new AddressRegistroImprese();
        response.setCode("NOT_FOUND");
        response.setDescription("No legal address");
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/national-registries-private/registro-imprese/address")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"filter\":{\"taxId\":\"" + VALID_PG_PIVA + "\"}}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.taxId").isEqualTo(VALID_PG_PIVA)
                .jsonPath("$.professionalAddress").doesNotExist();
    }

    /**
     * Verifica esito positivo quando il rappresentante legale InfoCamere coincide con quello richiesto.
     */
    @Test
    void InfoCamereLegal_MatchingRepresentativeReturnsTrue() {
        InfoCamereVerification response = new InfoCamereVerification();
        response.setCfPersona(VALID_PF_CF);
        response.setCfImpresa(VALID_PG_PIVA);
        response.setEsitoVerifica("OK");
        when(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(any())).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/national-registries-private/infocamere/legal")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(legalRequest())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.taxId").isEqualTo(VALID_PF_CF)
                .jsonPath("$.vatNumber").isEqualTo(VALID_PG_PIVA)
                .jsonPath("$.verificationResult").isEqualTo(true);
    }

    /**
     * Verifica esito negativo quando il rappresentante legale InfoCamere non coincide con quello richiesto.
     */
    @Test
    void InfoCamereLegal_NonMatchingRepresentativeReturnsFalse() {
        InfoCamereVerification response = new InfoCamereVerification();
        response.setCfPersona(VALID_PF_CF);
        response.setCfImpresa(VALID_PG_PIVA);
        response.setEsitoVerifica("KO");
        when(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(any())).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/national-registries-private/infocamere/legal")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(legalRequest())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.verificationResult").isEqualTo(false);
    }

    /**
     * Verifica che un errore applicativo nella verifica legale venga rappresentato come esito negativo.
     */
    @Test
    void InfoCamereLegal_ApplicationErrorReturnsRequestAsNegativeResult() {
        InfoCamereVerification response = new InfoCamereVerification();
        response.setCode("ERROR");
        response.setDescription("provider error");
        when(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(any())).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/national-registries-private/infocamere/legal")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(legalRequest())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.taxId").isEqualTo(VALID_PF_CF)
                .jsonPath("$.vatNumber").isEqualTo(VALID_PG_PIVA)
                .jsonPath("$.verificationResult").isEqualTo(false);
    }

    /**
     * Verifica che l’endpoint restituisca le imprese rappresentate dal soggetto richiesto.
     */
    @Test
    void InfoCamereLegalInstitutions_ReturnsRepresentedBusinesses() throws Exception {
        InfoCamereLegalInstituionsResponse response = new InfoCamereLegalInstituionsResponse();
        response.setCfPersona(VALID_PF_CF);
        response.setDataOraEstrazione(new Date().toString());
        setRepresentedBusinesses(response, "Impresa Test", VALID_PG_PIVA);
        when(infoCamereClient.getLegalInstitutions(any())).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/national-registries-private/infocamere/legal-institutions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"filter\":{\"taxId\":\"" + VALID_PF_CF + "\"}}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.legalTaxId").isEqualTo(VALID_PF_CF)
                .jsonPath("$.businessList.length()").isEqualTo(1)
                .jsonPath("$.businessList[0].businessName").isEqualTo("Impresa Test")
                .jsonPath("$.businessList[0].businessTaxId").isEqualTo(VALID_PG_PIVA);
    }

    /**
     * Verifica che una lista provider vuota produca una lista funzionale vuota.
     */
    @Test
    void InfoCamereLegalInstitutions_EmptyProviderListReturnsEmptyList() {
        InfoCamereLegalInstituionsResponse response = new InfoCamereLegalInstituionsResponse();
        response.setCfPersona(VALID_PF_CF);
        response.setDataOraEstrazione(new Date().toString());
        response.setElencoImpreseRappresentate(List.of());
        when(infoCamereClient.getLegalInstitutions(any())).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/national-registries-private/infocamere/legal-institutions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"filter\":{\"taxId\":\"" + VALID_PF_CF + "\"}}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.legalTaxId").isEqualTo(VALID_PF_CF)
                .jsonPath("$.businessList.length()").isEqualTo(0);
    }

    private String legalRequest() {
        return "{\"filter\":{\"taxId\":\"" + VALID_PF_CF + "\",\"vatNumber\":\"" + VALID_PG_PIVA + "\"}}";
    }

    private void setRepresentedBusinesses(InfoCamereLegalInstituionsResponse response, String name, String taxId) throws Exception {
        Method getter = response.getClass().getMethod("getElencoImpreseRappresentate");
        ParameterizedType returnType = (ParameterizedType) getter.getGenericReturnType();
        Class<?> itemType = (Class<?>) returnType.getActualTypeArguments()[0];
        Object item = itemType.getDeclaredConstructor().newInstance();
        itemType.getMethod("setDenominazione", String.class).invoke(item, name);
        itemType.getMethod("setCfImpresa", String.class).invoke(item, taxId);
        response.getClass().getMethod("setElencoImpreseRappresentate", List.class).invoke(response, List.of(item));
    }
}
