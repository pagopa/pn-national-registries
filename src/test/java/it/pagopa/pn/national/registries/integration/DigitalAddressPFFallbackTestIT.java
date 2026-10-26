package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import org.junit.jupiter.api.Test;
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
        "pn.national-registries.enable-inad-massive-api=false"
})
public class DigitalAddressPFFallbackTestIT extends AbstractNationalRegistriesComponentIT {

    @Test
    void PF_Digital_INAD_NotFound_InipecFallback() {
        String correlationId = "pf-digital-inad-not-found-fallback-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId);
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
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
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
