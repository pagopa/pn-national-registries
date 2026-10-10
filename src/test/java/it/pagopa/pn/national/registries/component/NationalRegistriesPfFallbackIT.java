package it.pagopa.pn.national.registries.component;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.inad-batch-enabled=false"
})
class NationalRegistriesPfFallbackIT extends AbstractNationalRegistriesComponentIT {


    @Test
    void PF_Digital_INAD_NotFound_Fallback() {
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

        List<Map<String, AttributeValue>> requests = findNationalRegistriesRequestsByCorrelationId(correlationId);
        assertEquals(1, requests.size());
        Map<String, AttributeValue> storedRequest = requests.get(0);
        assertEquals(correlationId, storedRequest.get("correlationId").s());
        assertEquals(VALID_PF_CF, storedRequest.get("taxId").s());
        assertEquals("PG", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals(CX_ID, storedRequest.get("clientId").s());
        assertEquals("INIPEC", storedRequest.get("registry").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());
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

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
    }

    @Test
    void PF_Digital_INAD_PersonalInsteadOfProfessional_fallbackEnabled() {
        String correlationId = "pf-digital-personal-enabled-" + UUID.randomUUID();
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

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("PROFESSIONALE", payload.getDigitalAddress().getFirst().getRecipient());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
    }

}

