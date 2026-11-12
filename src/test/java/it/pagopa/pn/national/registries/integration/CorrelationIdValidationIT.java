package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * PN-22205 / PN-22206 / PN-22207: il correlationId deve rispettare lo schema CorrelationId dell'OpenAPI.
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
class CorrelationIdValidationIT extends AbstractNationalRegistriesComponentIT {

    private static final String X_REQUEST_ID = "X-Request-ID";

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "<script>alert(document.cookie)</script>",
            "' OR '1'='1",
            "'OR'1'='1",
            "A URL injected http://injected.example.test with id URL-g9W43",
            "http://injected.example.test"
    })
    void iniPecDigitalAddress_invalidCorrelationId_returnsBadRequest(String correlationId) {
        webTestClient.post()
                .uri("/national-registries-private/inipec/digital-address")
                .header("pn-national-registries-cx-id", CX_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildIniPecRequest(correlationId, VALID_PG_PIVA))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().doesNotExist(X_REQUEST_ID);

        assertEquals(0, dynamoDbClient.scan(ScanRequest.builder().tableName(BATCH_REQUESTS_TABLE).build()).count());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "NATIONAL_REGISTRY_CALL.IUN_AKTK-JYUK-ZNKG-202610-M-1.RECINDEX_0.DELIVERYMODE_DIGITAL.CONTACTPHASE_CHOOSE_DELIVERY.ATTEMPT_0",
            "NRG_ADDRESS_SERVICE_DESK_OPID-ticket001",
            "3f2b9c1e-8d4a-4b7e-9f10-2c6a5d8e7b31",
            "client:id@domain#1+a=b"
    })
    void iniPecDigitalAddress_validCorrelationId_isAccepted(String correlationId) {
        webTestClient.post()
                .uri("/national-registries-private/inipec/digital-address")
                .header("pn-national-registries-cx-id", CX_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildIniPecRequest(correlationId, VALID_PG_PIVA))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(X_REQUEST_ID, correlationId);

        assertEquals(1, findBatchRequestsByCorrelationPrefix(correlationId).size());
    }

    @Test
    void addresses_invalidCorrelationId_returnsBadRequest() {
        webTestClient.post()
                .uri("/national-registries-private/PF/addresses")
                .header("pn-national-registries-cx-id", CX_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildGatewayRequest("<script>alert(1)</script>", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().doesNotExist(X_REQUEST_ID);

        verifyNoInteractions(inadClient, infoCamereClient, anprClient);
        assertEquals(0, receiveMessages(INPUT_QUEUE, 1).size());
    }
}
