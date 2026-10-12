package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecBatchResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.inad-batch-enabled=false"
})
class DigitalAddressPFFallbackTestIT extends AbstractNationalRegistriesComponentIT {

    @Test
    void PF_Digital_INAD_NotFound_Fallback_IniPecPecFound() {
        String correlationId = "pf-digital-not-found-inipec-found-" + UUID.randomUUID();
        String pollingId = "polling-" + UUID.randomUUID();

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        // Setup mocks for inipec batch request and polling
        IniPecBatchResponse batchResponse = iniPecBatchResponse(pollingId);
        Pec pec = pecResponse(VALID_PF_CF, "fallback@pec.it");
        IniPecPollingResponse pollingResponse = iniPecPollingResponse(pec);

        when(infoCamereClient.callEServiceRequestId(any())).thenReturn(Mono.just(batchResponse));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));

        List<Map<String, AttributeValue>> requests = findNationalRegistriesRequestsByCorrelationId(correlationId);
        assertEquals(1, requests.size());
        Map<String, AttributeValue> storedRequest = requests.get(0);
        assertEquals(correlationId, storedRequest.get("correlationId").s());
        assertEquals(VALID_PF_CF, storedRequest.get("taxId").s());
        assertEquals("PF", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals(CX_ID, storedRequest.get("clientId").s());
        assertEquals("INIPEC", storedRequest.get("registry").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());

        // Process inipec batch request and polling
        iniPecBatchRequestService.batchPecRequest();
        digitalAddressBatchPollingService.batchPecPolling();

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("fallback@pec.it", payload.getDigitalAddress().get(0).getAddress());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verify(infoCamereClient).callEServiceRequestId(any());
        verify(infoCamereClient).callEServiceRequestPec(pollingId);
    }

    @Test
    void PF_Digital_INAD_NotFound_Fallback_IniPecMultiplePecReturned() {
        String correlationId = "pf-digital-not-found-inipec-multiple-pec-" + UUID.randomUUID();
        String pollingId = "polling-" + UUID.randomUUID();

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        List<Map<String, AttributeValue>> requests = findNationalRegistriesRequestsByCorrelationId(correlationId);
        assertEquals(1, requests.size());
        Map<String, AttributeValue> storedRequest = requests.get(0);
        assertEquals(correlationId, storedRequest.get("correlationId").s());
        assertEquals(VALID_PF_CF, storedRequest.get("taxId").s());
        assertEquals("PF", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals(CX_ID, storedRequest.get("clientId").s());
        assertEquals("INIPEC", storedRequest.get("registry").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());


        // Setup mocks for inipec batch request and polling with multiple PEC addresses
        IniPecBatchResponse batchResponse = iniPecBatchResponse(pollingId);
        Pec pec1 = pecResponse(VALID_PF_CF, "pec1@pec.it");
        Pec pec2 = pecResponse(VALID_PF_CF, "pec2@pec.it");
        IniPecPollingResponse pollingResponse = iniPecPollingResponse(pec1, pec2);

        when(infoCamereClient.callEServiceRequestId(any())).thenReturn(Mono.just(batchResponse));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));

        // Process inipec batch request and polling
        iniPecBatchRequestService.batchPecRequest();
        digitalAddressBatchPollingService.batchPecPolling();

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        // Should return the first PEC found
        assertEquals("pec1@pec.it", payload.getDigitalAddress().get(0).getAddress());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verify(infoCamereClient).callEServiceRequestId(any());
        verify(infoCamereClient).callEServiceRequestPec(pollingId);
    }

    @Test
    void PF_Digital_INAD_PersonalInvalid_ProfessionalValid_fallbackEnabled_IniPecPecFound() {
        String correlationId = "pf-digital-valid-pec-inipec-found-" + UUID.randomUUID();
        String pollingId = "polling-" + UUID.randomUUID();

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("2026-01-01", null),
                        inadAddress("professional@pec.it", "INGEGNERE")
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        // No batch request should be created since professional PEC is valid
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertEquals("professional@pec.it", payload.getDigitalAddress().get(0).getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().get(0).getRecipient());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    void PF_Digital_INAD_PersonalInsteadOfProfessional_fallbackEnabled() {
        String correlationId = "pf-digital-personal-enabled-inipec-notfound-" + UUID.randomUUID();

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("personal@pec.it", null),
                        inadAddress("professional@pec.it", "INGEGNERE")
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        // No batch request should be created since personal PEC is valid and returned
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertEquals("personal@pec.it", payload.getDigitalAddress().get(0).getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().get(0).getRecipient());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    void PF_Digital_INAD_MultipleProfessionals_fallbackEnabled() {
        String correlationId = "pf-digital-multiple-prof-enabled-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("prof1@pec.it", "INGEGNERE"),
                        inadAddress("prof2@pec.it", "ARCHITETTO")
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("prof1@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void PF_Digital_INAD_EmptyList_fallbackEnabled() {
        String correlationId = "pf-digital-empty-list-enabled-" + UUID.randomUUID();
        ResponseRequestDigitalAddress emptyResponse = new ResponseRequestDigitalAddress();
        emptyResponse.setCodiceFiscale(VALID_PF_CF);
        emptyResponse.setSince(java.util.Date.from(java.time.Instant.parse("2026-10-10T10:15:30Z")));
        emptyResponse.setDigitalAddress(List.of());

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(emptyResponse));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNull(outputMessage);

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        java.util.List<java.util.Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue>> requests =
                findNationalRegistriesRequestsByCorrelationId(correlationId);
        assertEquals(1, requests.size());
        assertEquals("INIPEC", requests.get(0).get("registry").s());
        assertEquals("NOT_WORKED", requests.get(0).get("status").s());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void PF_Digital_INAD_BothInvalid_fallbackEnabled() {
        String correlationId = "pf-digital-both-invalid-enabled-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("2026-01-01", null),
                        inadAddress("2026-01-02", "INGEGNERE")
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNull(outputMessage);

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        java.util.List<java.util.Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue>> requests =
                findNationalRegistriesRequestsByCorrelationId(correlationId);
        assertEquals(1, requests.size());
        java.util.Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> storedRequest = requests.get(0);
        assertEquals(correlationId, storedRequest.get("correlationId").s());
        assertEquals(VALID_PF_CF, storedRequest.get("taxId").s());
        assertEquals("PF", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals("INIPEC", storedRequest.get("registry").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void PF_Digital_INAD_BothInvalid_fallbackEnabled_IniPecPecNotFound() {
        String correlationId = "pf-digital-both-invalid-inipec-notfound-" + UUID.randomUUID();
        String pollingId = "polling-" + UUID.randomUUID();

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("2026-01-01", null),
                        inadAddress("2026-01-02", "INGEGNERE")
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        // Setup mocks for inipec batch request and polling returning no results
        IniPecBatchResponse batchResponse = iniPecBatchResponse(pollingId);
        IniPecPollingResponse pollingResponse = iniPecPollingResponse();

        when(infoCamereClient.callEServiceRequestId(any())).thenReturn(Mono.just(batchResponse));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));

        // Process inipec batch request and polling
        iniPecBatchRequestService.batchPecRequest();
        digitalAddressBatchPollingService.batchPecPolling();

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verify(infoCamereClient).callEServiceRequestId(any());
        verify(infoCamereClient).callEServiceRequestPec(pollingId);
    }


    @Test
    void PF_Digital_INAD_NotFound_Fallback_IniPecEmptyPolling() {
        String correlationId = "pf-digital-not-found-inipec-empty-polling-" + UUID.randomUUID();
        String pollingId = "polling-" + UUID.randomUUID();

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        // Setup mocks for inipec batch request with empty polling response
        IniPecBatchResponse batchResponse = iniPecBatchResponse(pollingId);
        IniPecPollingResponse emptyPollingResponse = new IniPecPollingResponse();
        emptyPollingResponse.setElencoPec(List.of());

        when(infoCamereClient.callEServiceRequestId(any())).thenReturn(Mono.just(batchResponse));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(emptyPollingResponse));

        // Process inipec batch request and polling
        iniPecBatchRequestService.batchPecRequest();
        digitalAddressBatchPollingService.batchPecPolling();

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verify(infoCamereClient).callEServiceRequestId(any());
        verify(infoCamereClient).callEServiceRequestPec(pollingId);
    }

    @Test
    void PF_Digital_INAD_PersonalNotFound_ProfessionalFound_fallbackEnabled() {
        String correlationId = "pf-digital-professional-only-enabled-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("professional@pec.it", "INGEGNERE")
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());


        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
    }

    @Test
    void PF_Digital_INAD_PersonalInvalid_ProfessionalValid_fallbackEnabled() {
        String correlationId = "pf-digital-valid-pec-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("2026-01-01", null),
                        inadAddress("professional@pec.it", "INGEGNERE")
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());


        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
    }

    @Test
    void PF_Digital_INAD_Error_fallbackEnabled() {
        String correlationId = "pf-digital-error-enabled-" + UUID.randomUUID();
        RuntimeException technicalError = new RuntimeException("INAD unavailable");
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertEquals(correlationId, response.getCorrelationId());
        assertSame(technicalError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void PF_Digital_INAD_NotFound_Fallback_IniPecBatchRequestError() {
        String correlationId = "pf-digital-not-found-inipec-batchtreqerror-" + UUID.randomUUID();

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        // Setup mock for inipec batch request error
        RuntimeException batchError = new RuntimeException("Batch request failed");
        when(infoCamereClient.callEServiceRequestId(any())).thenReturn(Mono.error(batchError));

        // Process inipec batch request - should handle error gracefully
        iniPecBatchRequestService.batchPecRequest();

        // Verify batch request was attempted
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verify(infoCamereClient).callEServiceRequestId(any());
    }

    @Test
    void PF_Digital_INAD_NotFound_Fallback_IniPecPollingError() {
        String correlationId = "pf-digital-not-found-inipec-pollingerror-" + UUID.randomUUID();
        String pollingId = "polling-" + UUID.randomUUID();

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        // Setup mocks for inipec batch request success but polling error
        IniPecBatchResponse batchResponse = iniPecBatchResponse(pollingId);
        RuntimeException pollingError = new RuntimeException("Polling failed");

        when(infoCamereClient.callEServiceRequestId(any())).thenReturn(Mono.just(batchResponse));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(pollingError));

        // Process inipec batch request
        iniPecBatchRequestService.batchPecRequest();
        // Process inipec polling - should handle error gracefully
        digitalAddressBatchPollingService.batchPecPolling();

        // Verify interactions
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verify(infoCamereClient).callEServiceRequestId(any());
        verify(infoCamereClient).callEServiceRequestPec(pollingId);
    }
}
