package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=false",
        "pn.national-registries.enable-inad-massive-api=false",
        "spring.task.scheduling.enabled=false"
})
public class DigitalAddressPFTestIT extends AbstractNationalRegistriesComponentIT {

    @Test
    void PF_Digital_INAD() {
        String correlationId = "pf-digital-found-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, "pf@pec.it", null)));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue(), payload.getAddressType());
        assertNotNull(payload.getDigitalAddress());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("pf@pec.it", payload.getDigitalAddress().get(0).getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().get(0).getRecipient());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void PF_Digital_INAD_NotFound_NoFallback() {
        String correlationId = "pf-digital-not-found-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void PF_Digital_INAD_PersonalNotFound_ProfessionalFound_fallbackDisabled() {
        String correlationId = "pf-digital-professional-only-disabled-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
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
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void PF_Digital_INAD_PersonalInsteadOfProfessional_fallbackDisabled() {
        String correlationId = "pf-digital-personal-preferred-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("personal@pec.it", null),
                        inadAddress("professional@pec.it", "INGEGNERE")
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("personal@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
    }

    @Test
    void PF_Digital_INAD_PersonalInvalid_ProfessionalValid_fallbackDisable() {
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

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void PF_Digital_INAD_MultipleProfessionals_fallbackDisabled() {
        String correlationId = "pf-digital-multiple-prof-disabled-" + UUID.randomUUID();
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

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void PF_Digital_INAD_EmptyList_fallbackDisabled() {
        String correlationId = "pf-digital-empty-list-disabled-" + UUID.randomUUID();

        ResponseRequestDigitalAddress emptyResponse = new ResponseRequestDigitalAddress();
        emptyResponse.setCodiceFiscale(VALID_PF_CF);
        emptyResponse.setSince(Date.from(Instant.parse("2026-10-10T10:15:30Z")));
        emptyResponse.setDigitalAddress(List.of());

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(emptyResponse));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void PF_Digital_INAD_Error() {
        String correlationId = "pf-digital-technical-disabled-" + UUID.randomUUID();
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
    void PF_Digital_INAD_PersonalExpired_ProfessionalValid_fallbackDisabled() {
        String correlationId = "pf-digital-expired-personal-professional-" + UUID.randomUUID();

        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
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
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void PF_Digital_INAD_PersonalExpired_fallbackDisabled() {
        String correlationId = "pf-digital-expired-personal-" + UUID.randomUUID();

        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        expiredPersonal
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }
}