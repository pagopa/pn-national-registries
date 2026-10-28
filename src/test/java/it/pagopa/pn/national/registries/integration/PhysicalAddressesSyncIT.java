package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.AddressRegistroImprese;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
class PhysicalAddressesSyncIT extends AbstractNationalRegistriesComponentIT {

    @Test
    void PhysicalAddresses_EmptyAddressesReturnsBadRequest() {
        webTestClient.post()
                .uri("/national-registries-private/physical-addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"correlationId":"corr-empty","referenceRequestDate":"2026-10-10T10:15:30.000Z","addresses":[]}
                        """)
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(anprClient, infoCamereClient);
    }

    @Test
    void PhysicalAddresses_MixedPfPgReturnsIndependentResultsInInputOrder() {
        String correlationId = "physical-mixed-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(VALID_PF_CF, "VIA", "ROMA", "10", "Roma", "RM", "00100")));
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "MILANO", "20", "Milano", "MI", "20100")));

        webTestClient.post()
                .uri("/national-registries-private/physical-addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(correlationId,
                        "{\"recIndex\":1,\"taxId\":\"" + VALID_PF_CF + "\",\"recipientType\":\"PF\"}",
                        "{\"recIndex\":2,\"taxId\":\"" + VALID_PG_PIVA + "\",\"recipientType\":\"PG\"}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.correlationId").isEqualTo(correlationId)
                .jsonPath("$.addresses.length()").isEqualTo(2)
                .jsonPath("$.addresses[?(@.recIndex == 1)].registry").isEqualTo(List.of("ANPR"))
                .jsonPath("$.addresses[?(@.recIndex == 1)].physicalAddress.address").isEqualTo(List.of("VIA ROMA 10"))
                .jsonPath("$.addresses[?(@.recIndex == 2)].registry").isEqualTo(List.of("REGISTRO_IMPRESE"))
                .jsonPath("$.addresses[?(@.recIndex == 2)].physicalAddress.address").isEqualTo(List.of("VIA MILANO 20"));
    }

    @Test
    void PhysicalAddresses_AnprNotFoundDoesNotFailOtherElements() {
        String correlationId = "physical-anpr-not-found-" + UUID.randomUUID();
        PnNationalRegistriesException notFound = new PnNationalRegistriesException("Not Found", 404, "Not Found", HttpHeaders.EMPTY,
                "{\"codiceErroreAnomalia\":\"EN122\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8, AnprResponseKO.class);
        when(anprClient.callEService(any())).thenReturn(Mono.error(notFound));
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "MILANO", "20", "Milano", "MI", "20100")));

        webTestClient.post()
                .uri("/national-registries-private/physical-addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(correlationId,
                        "{\"recIndex\":1,\"taxId\":\"" + VALID_PF_CF + "\",\"recipientType\":\"PF\"}",
                        "{\"recIndex\":2,\"taxId\":\"" + VALID_PG_PIVA + "\",\"recipientType\":\"PG\"}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.addresses[?(@.recIndex == 1)].registry").isEqualTo(List.of("ANPR"))
                .jsonPath("$.addresses[?(@.recIndex == 1)].physicalAddress").isEqualTo(List.of())
                .jsonPath("$.addresses[?(@.recIndex == 1)].error").isEqualTo(List.of())
                .jsonPath("$.addresses[?(@.recIndex == 2)].physicalAddress.address").isEqualTo(List.of("VIA MILANO 20"));
    }

    @Test
    void PhysicalAddresses_TooManyRequestsIsReportedOnlyOnFailedElement() {
        String correlationId = "physical-429-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.error(new PnNationalRegistriesException(
                "too many", 429, "Too Many Requests", null, null, StandardCharsets.UTF_8, AnprResponseKO.class)));
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "MILANO", "20", "Milano", "MI", "20100")));

        webTestClient.post()
                .uri("/national-registries-private/physical-addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(correlationId,
                        "{\"recIndex\":1,\"taxId\":\"" + VALID_PF_CF + "\",\"recipientType\":\"PF\"}",
                        "{\"recIndex\":2,\"taxId\":\"" + VALID_PG_PIVA + "\",\"recipientType\":\"PG\"}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.addresses[?(@.recIndex == 1)].error").isEqualTo(List.of("DOWNSTREAM_TOO_MANY_REQUESTS"))
                .jsonPath("$.addresses[?(@.recIndex == 1)].errorStatus").isEqualTo(List.of(429))
                .jsonPath("$.addresses[?(@.recIndex == 2)].physicalAddress.address").isEqualTo(List.of("VIA MILANO 20"));
    }

    @Test
    void PhysicalAddresses_GenericFailureIsReportedOnlyOnFailedElement() {
        String correlationId = "physical-generic-error-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.error(new IllegalStateException("ANPR unavailable")));
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "MILANO", "20", "Milano", "MI", "20100")));

        webTestClient.post()
                .uri("/national-registries-private/physical-addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(correlationId,
                        "{\"recIndex\":1,\"taxId\":\"" + VALID_PF_CF + "\",\"recipientType\":\"PF\"}",
                        "{\"recIndex\":2,\"taxId\":\"" + VALID_PG_PIVA + "\",\"recipientType\":\"PG\"}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.addresses[?(@.recIndex == 1)].error").isEqualTo(List.of("DOWNSTREAM_REQUEST_ERROR"))
                .jsonPath("$.addresses[?(@.recIndex == 1)].errorStatus").isEqualTo(List.of(500))
                .jsonPath("$.addresses[?(@.recIndex == 2)].physicalAddress.address").isEqualTo(List.of("VIA MILANO 20"));
    }

    @Test
    void PhysicalAddresses_PgWithoutLegalAddressReturnsElementWithoutPhysicalAddress() {
        String correlationId = "physical-pg-empty-" + UUID.randomUUID();
        AddressRegistroImprese response = new AddressRegistroImprese();
        response.setCf(VALID_PG_PIVA);
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/national-registries-private/physical-addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(correlationId, "{\"recIndex\":7,\"taxId\":\"" + VALID_PG_PIVA + "\",\"recipientType\":\"PG\"}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.addresses[0].recIndex").isEqualTo(7)
                .jsonPath("$.addresses[0].registry").isEqualTo("REGISTRO_IMPRESE")
                .jsonPath("$.addresses[0].physicalAddress").isMap()
                .jsonPath("$.addresses[0].physicalAddress").isEmpty();
    }

    private String request(String correlationId, String... addresses) {
        return "{\"correlationId\":\"" + correlationId + "\",\"referenceRequestDate\":\"2026-10-10T10:15:30.000Z\",\"addresses\":[" + String.join(",", addresses) + "]}";
    }
}