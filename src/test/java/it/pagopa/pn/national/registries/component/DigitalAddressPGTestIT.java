package it.pagopa.pn.national.registries.component;

import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecBatchResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressIniPECOKDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
public class DigitalAddressPGTestIT extends AbstractNationalRegistriesComponentIT {
    @Test
    void PG_Digital_IPA() {
        String correlationId = "pg-digital-found-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), anyString())).thenReturn(Mono.just(ipaFoundResponse(VALID_PG_PIVA, "pg@ipa.pec.it")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("IPA", payload.getRegistry());
        assertEquals("pg@ipa.pec.it", payload.getDigitalAddress().get(0).getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().get(0).getRecipient());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA), anyString());
        verifyNoInteractions(inadClient, anprClient);
    }

    @Test
    void PG_Digital_IPA_NotFound_Inipec() {
        String correlationId = "pg-digital-inipec-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), anyString())).thenReturn(Mono.just(ipaNotFoundResponse()));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        List<Map<String, AttributeValue>> requests = findNationalRegistriesRequestsByCorrelationId(correlationId);
        assertEquals(1, requests.size());
        Map<String, AttributeValue> storedRequest = requests.get(0);
        assertEquals(correlationId, storedRequest.get("correlationId").s());
        assertEquals(VALID_PG_PIVA, storedRequest.get("taxId").s());
        assertEquals("PG", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals(CX_ID, storedRequest.get("clientId").s());
        assertEquals("INIPEC", storedRequest.get("registry").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());

        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA), anyString());
        verifyNoInteractions(inadClient, anprClient);
    }

    @Test
    void PG_Digital_IPA_Error() {
        String correlationId = "pg-digital-technical-" + UUID.randomUUID();
        RuntimeException technicalError = new RuntimeException("IPA unavailable");
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), anyString())).thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertEquals(correlationId, response.getCorrelationId());
        assertSame(technicalError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA), anyString());
        verifyNoInteractions(inadClient, anprClient);
    }

    @Test
    void iniPecBatchFlowShouldPublishIniPecResultAndStopWhenPecFound() {
        String originalCorrelationId = "inipec-stop-" + UUID.randomUUID();
        GetDigitalAddressIniPECOKDto enqueueResponse = enqueueIniPecRequest(originalCorrelationId, VALID_PG_PIVA);
        String storedCorrelationId = enqueueResponse.getCorrelationId();
        String pollingId = "polling-" + UUID.randomUUID();

        IniPecBatchResponse batchResponse = iniPecBatchResponse(pollingId);
        Pec pec = pecResponse(VALID_PG_PIVA, "inipec@pec.it");
        IniPecPollingResponse pollingResponse = iniPecPollingResponse(pec);

        when(infoCamereClient.callEServiceRequestId(any())).thenReturn(Mono.just(batchResponse));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));

        iniPecBatchRequestService.batchPecRequest();
        digitalAddressBatchPollingService.batchPecPolling();

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(originalCorrelationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("inipec@pec.it", payload.getDigitalAddress().get(0).getAddress());

        Map<String, AttributeValue> storedItem = getBatchRequestItem(storedCorrelationId);
        assertEquals("WORKED", storedItem.get("status").s());
        assertEquals("SENT", storedItem.get("sendStatus").s());
        assertEquals("INIPEC", storedItem.get("eservice").s());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(infoCamereClient).callEServiceRequestId(any());
        verify(infoCamereClient).callEServiceRequestPec(pollingId);
        verifyNoInteractions(inadClient, anprClient, ipaClient);
    }

    @Test
    void iniPecBatchFlowShouldPublishEmptyIniPecResultAndStopForPfWhenPecIsMissing() {
        String originalCorrelationId = "inipec-pf-empty-" + UUID.randomUUID();
        GetDigitalAddressIniPECOKDto enqueueResponse = enqueueIniPecRequest(originalCorrelationId, VALID_PF_CF);
        String storedCorrelationId = enqueueResponse.getCorrelationId();
        String pollingId = "polling-" + UUID.randomUUID();

        when(infoCamereClient.callEServiceRequestId(any())).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse()));

        iniPecBatchRequestService.batchPecRequest();
        digitalAddressBatchPollingService.batchPecPolling();

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(originalCorrelationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());

        Map<String, AttributeValue> storedItem = getBatchRequestItem(storedCorrelationId);
        assertEquals("WORKED", storedItem.get("status").s());
        assertEquals("SENT", storedItem.get("sendStatus").s());
        assertEquals("INIPEC", storedItem.get("eservice").s());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verifyNoInteractions(inadClient, anprClient, ipaClient);
    }

    @Test
    void iniPecBatchFlowShouldFallbackToInadForPgWhenPecIsMissing() {
        String originalCorrelationId = "inipec-pg-inad-" + UUID.randomUUID();
        GetDigitalAddressIniPECOKDto enqueueResponse = enqueueIniPecRequest(originalCorrelationId, VALID_PG_PIVA);
        String storedCorrelationId = enqueueResponse.getCorrelationId();
        String pollingId = "polling-" + UUID.randomUUID();

        when(infoCamereClient.callEServiceRequestId(any())).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse()));
        when(inadClient.callEService(eq(VALID_PG_PIVA), eq(storedCorrelationId))).thenReturn(Mono.just(inadResponse(VALID_PG_PIVA, "fallback@pec.it", null)));

        iniPecBatchRequestService.batchPecRequest();
        digitalAddressBatchPollingService.batchPecPolling();

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(originalCorrelationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertEquals("fallback@pec.it", payload.getDigitalAddress().get(0).getAddress());

        Map<String, AttributeValue> storedItem = getBatchRequestItem(storedCorrelationId);
        assertEquals("WORKED", storedItem.get("status").s());
        assertEquals("SENT", storedItem.get("sendStatus").s());
        assertEquals("INAD", storedItem.get("eservice").s());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(eq(VALID_PG_PIVA), eq(storedCorrelationId));
        verifyNoInteractions(anprClient, ipaClient);
    }
}
