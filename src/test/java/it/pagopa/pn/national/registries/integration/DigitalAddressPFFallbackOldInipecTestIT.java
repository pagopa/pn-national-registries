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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.enable-old-inipec-flow=true",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0"
})
public class DigitalAddressPFFallbackOldInipecTestIT extends AbstractNationalRegistriesComponentIT {

    @Test
    void PF_Digital_INAD_NotFound_OldInipecFallback() {
        String correlationId = "pf-old-inipec-fallback-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        List<Map<String, AttributeValue>> legacyRequests = findBatchRequestsByCorrelationPrefix(correlationId);
        assertEquals(1, legacyRequests.size());
        Map<String, AttributeValue> legacyRequest = legacyRequests.getFirst();
        assertTrue(legacyRequest.get("correlationId").s().startsWith(correlationId + "~"));
        assertEquals(VALID_PF_CF, legacyRequest.get("cf").s());
        assertEquals("PF", legacyRequest.get("recipientType").s());
        assertEquals("NOT_WORKED", legacyRequest.get("status").s());
        String pollingId = "pf-old-polling-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();

        legacyRequest = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKING", legacyRequest.get("status").s());
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pecResponse(VALID_PF_CF, "pf-old-final@pec.it"))));
        digitalAddressBatchPollingService.batchPecPolling();

        legacyRequest = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", legacyRequest.get("status").s());
        CodeSqsDto payload = readOutputMessage(legacyRequest.get("message").s());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("pf-old-final@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verify(ipaClient, never()).callEServiceWS23(any(), any());
        verifyNoInteractions(anprClient);
    }
}
