package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.service.InipecPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Instant;
import java.util.Date;
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
        "pn.national-registries.enable-old-inipec-flow=false",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "spring.task.scheduling.enabled=false"
})
public class DigitalAddressPFFallbackTestIT extends AbstractNationalRegistriesComponentIT {

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private InipecPollingService inipecPollingService;

    @Test
    void PF_Digital_INAD_NotFound_InipecFallback() {
        String correlationId = "pf-digital-inad-not-found-fallback-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId);
        completeInipecFinal(correlationId, "inipec-not-found-fallback@pec.it");
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void shouldTakeProfessionalWhenPersonalExpired() {
        String correlationId = "pf-digital-expired-personal-professional-fallback-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                expiredPersonal,
                inadAddress("professional@pec.it", "INGEGNERE")
        ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());

        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void shouldFallbackToInipecWhenPersonalExpired() {
        String correlationId = "pf-digital-expired-personal-fallback-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                expiredPersonal
        ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId);
        completeInipecFinal(correlationId, "inipec-after-expired-pending@pec.it");
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }


    @Test
    void PF_Digital_INAD_PersonalFound_NoInipecFallback() {
        String correlationId = "pf-digital-personal-found-fallback-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PF_CF, "personal@pec.it", null)));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("personal@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    @Test
    void PF_Digital_INAD_PersonalInvalid_ProfessionalValid_FallbackEnabled() {
        String correlationId = "pf-digital-invalid-personal-professional-fallback-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                inadAddress("invalid-personal", null),
                inadAddress("professional@pec.it", "INGEGNERE")
        ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }


    @Test
    void PF_Digital_INAD_TechnicalError_NoInipecFallback() {
        String correlationId = "pf-digital-technical-fallback-" + UUID.randomUUID();
        RuntimeException technicalError = new RuntimeException("INAD unavailable");
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        assertEquals(correlationId, response.getCorrelationId());
        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertSame(technicalError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }


    @Test
    void PF_Digital_INAD_ExpiredPersonal_InipecValid_FullFlow() {
        String correlationId = "pf-digital-expired-personal-inipec-valid-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(expiredPersonal))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId);

        String pollingId = "inipec-expired-pf-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();

        Map<String, AttributeValue> inipecRequest = findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> "INIPEC".equals(request.get("registry").s()))
                .findFirst()
                .orElseThrow();
        String batchId = inipecRequest.get("batchId").s();
        pollingJob.polling();

        IniPecPollingResponse pollingResponse = new IniPecPollingResponse();
        pollingResponse.setElencoPec(List.of(pecResponse(VALID_PF_CF, "inipec-after-expired@pec.it")));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));

        inipecPollingService.handleMessage(PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INIPEC)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(1)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build()).block();

        inipecRequest = findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> "INIPEC".equals(request.get("registry").s()))
                .findFirst()
                .orElseThrow();
        assertEquals("WORKED", inipecRequest.get("status").s());
        assertNotNull(inipecRequest.get("resultPayload"));
        CodeSqsDto payload = readOutputMessage(inipecRequest.get("resultPayload").s());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("inipec-after-expired@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    private void completeInipecFinal(String correlationId, String address) {
        String pollingId = "inipec-full-flow-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        Map<String, AttributeValue> request = findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(item -> "INIPEC".equals(item.get("registry").s()))
                .findFirst()
                .orElseThrow();
        String batchId = request.get("batchId").s();
        pollingJob.polling();
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pecResponse(VALID_PF_CF, address))));
        inipecPollingService.handleMessage(PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INIPEC)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(1)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build()).block();
        request = findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(item -> "INIPEC".equals(item.get("registry").s()))
                .findFirst()
                .orElseThrow();
        assertEquals("WORKED", request.get("status").s());
        assertNotNull(request.get("resultPayload"));
        CodeSqsDto payload = readOutputMessage(request.get("resultPayload").s());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals(address, payload.getDigitalAddress().getFirst().getAddress());
    }

    private void assertInipecPendingRequest(String correlationId) {
        List<Map<String, AttributeValue>> storedRequests = findNationalRegistriesRequestsByCorrelationId(correlationId);
        assertEquals(1, storedRequests.size());
        Map<String, AttributeValue> storedRequest = storedRequests.getFirst();
        assertEquals(VALID_PF_CF, storedRequest.get("taxId").s());
        assertEquals("PF", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals("INIPEC", storedRequest.get("registry").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }
}
