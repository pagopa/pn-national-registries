package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.enable-old-inipec-flow=true",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
})
public class DigitalAddressPGOldInipecTestIT extends AbstractNationalRegistriesComponentIT {

    @Test
    void PG_Digital_IPA_NotFound_OldInipecFallback() {
        String correlationId = "pg-old-inipec-fallback-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), any())).thenReturn(Mono.just(ipaNotFoundResponse()));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        List<Map<String, AttributeValue>> legacyRequests = findBatchRequestsByCorrelationPrefix(correlationId);
        assertEquals(1, legacyRequests.size());
        Map<String, AttributeValue> legacyRequest = legacyRequests.getFirst();
        assertTrue(legacyRequest.get("correlationId").s().startsWith(correlationId + "~"));
        assertEquals(VALID_PG_PIVA, legacyRequest.get("cf").s());
        assertEquals("PG", legacyRequest.get("recipientType").s());
        assertEquals("NOT_WORKED", legacyRequest.get("status").s());
        String pollingId = "pg-old-polling-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();

        legacyRequest = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKING", legacyRequest.get("status").s());
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pecResponse(VALID_PG_PIVA, "pg-old-final@pec.it"))));
        digitalAddressBatchPollingService.batchPecPolling();

        legacyRequest = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", legacyRequest.get("status").s());
        CodeSqsDto payload = readOutputMessage(legacyRequest.get("message").s());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("pg-old-final@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA), any());
        verifyNoInteractions(inadClient, anprClient);
    }
}
