package it.pagopa.pn.national.registries.component;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=false",
        "pn.national-registries.inad-batch-enabled=false"
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

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
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

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
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
        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertEquals(correlationId, response.getCorrelationId());
        assertNotNull(thrown);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
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

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
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

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
        assertEquals("valid@pec.it", payload.getDigitalAddress().getFirst().getAddress());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
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
}
