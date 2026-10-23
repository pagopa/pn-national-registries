package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.AddressRegistroImprese;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false"
})
class PhysicalAddressTestIT extends AbstractNationalRegistriesComponentIT {

    @Test
    void PF_Physical_ANPR() {
        String correlationId = "pf-physical-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(VALID_PF_CF, "VIA", "ROMA", "10", "Roma", "RM", "00100")));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("ANPR", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(), payload.getAddressType());
        assertPhysicalAddress(payload,
                "VIA ROMA 10",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    void PF_Physical_ANPR_WithAllAddressDetails() {
        String correlationId = "pf-physical-full-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "ROMA MAGGIORE", "10", "Roma", "RM", "00100",
                "Scala A", "Presso Mario Rossi", "Roma Capitale"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("ANPR", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(), payload.getAddressType());
        assertPhysicalAddress(payload,
                "VIA ROMA MAGGIORE 10",
                "Scala A",
                "Presso Mario Rossi",
                "00100",
                "Roma",
                "RM",
                null,
                "Roma Capitale");

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    void PF_Physical_ANPR_WithPartialAddressDetails() {
        String correlationId = "pf-physical-partial-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "ROMA", "10", "Roma", "RM", "00100",
                "Scala B", null, null
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA ROMA 10",
                "Scala B",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    void PF_Physical_ANPR_WithAccentedCharacters() {
        String correlationId = "pf-physical-accented-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "SANT'AGATA", "22", "Napoli", "NA", "80100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA SANT'AGATA 22",
                "",
                null,
                "80100",
                "Napoli",
                "NA",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    @Test
    void PF_Physical_ANPR_WithNonStandardCivicNumber() {
        String correlationId = "pf-physical-nonstandard-civic-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "ROMA", "22", "B", null, "1", null,
                null, null, null, null, "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA ROMA 22/B SNC",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    void PF_Physical_ANPR_WithVeryLongStreetName() {
        String correlationId = "pf-physical-long-street-" + UUID.randomUUID();
        String longStreetName = "MOLTO LUNGA DEL COMUNE DI ROMA CON NOME PARTICOLARMENTE LUNGO E COMPLESSO CHE CONTIENE MOLTE PAROLE";
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", longStreetName, "100", "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA 100",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    void PF_Physical_ANPR_NotFound() {
        String correlationId = "pf-physical-not-found-" + UUID.randomUUID();

        PnNationalRegistriesException exception = new PnNationalRegistriesException("Not Found", 404, "Not Found", HttpHeaders.EMPTY, "{\"codiceErroreAnomalia\":\"EN122\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8, AnprResponseKO.class);
        when(anprClient.callEService(any())).thenReturn(Mono.error(exception));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("ANPR", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(), payload.getAddressType());
        assertNull(payload.getPhysicalAddress());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    void PF_Physical_ANPR_error() {
        String correlationId = "pf-physical-technical-" + UUID.randomUUID();
        RuntimeException technicalError = new RuntimeException("ANPR unavailable");
        when(anprClient.callEService(any())).thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertEquals(correlationId, response.getCorrelationId());
        assertSame(technicalError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    void PG_Physical_RegistroImprese() {
        String correlationId = "pg-physical-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "Milano", "25", "Milano", "MI", "20100")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("REGISTRO_IMPRESE", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(), payload.getAddressType());
        assertPhysicalAddress(payload,
                "VIA Milano 25",
                null,
                null,
                "20100",
                "Milano",
                "MI",
                "IT",
                null);

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);
    }

    @Test
    void PG_Physical_RegistroImprese_WithAllFields() {
        String correlationId = "pg-physical-full-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "REPUBBLICA", "100", "Milano", "MI", "20100", "IT")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA REPUBBLICA 100",
                null,
                null,
                "20100",
                "Milano",
                "MI",
                "IT",
                null);

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);
    }

    @Test
    void PG_Physical_RegistroImprese_WithForeignCountry() {
        String correlationId = "pg-physical-foreign-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "AVENUE", "EUROPA", "5", "Bruxelles", "BE", "1000", "BE")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "AVENUE EUROPA 5",
                null,
                null,
                "1000",
                "Bruxelles",
                "BE",
                "BE",
                null);

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
    }

    @Test
    void PG_Physical_RegistroImprese_WithAccentedCharacters() {
        String correlationId = "pg-physical-accented-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "SANT'AGATA D'ESARO", "15", "Reggio Calabria", "RC", "89100")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA SANT'AGATA D'ESARO 15",
                null,
                null,
                "89100",
                "Reggio Calabria",
                "RC",
                "IT",
                null);

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
    }

    @Test
    void PG_Physical_RegistroImprese_WithSecondaryAddress() {
        String correlationId = "pg-physical-secondary-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "SECONDARIA", "50", "Milano", "MI", "20100")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA SECONDARIA 50",
                null,
                null,
                "20100",
                "Milano",
                "MI",
                "IT",
                null);

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);
    }

    @Test
    void Physical_Address_WithInternationalPostalCode_UK() {
        String correlationId = "physical-international-uk-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "STREET", "LONDON ROAD", "42", "London", "LDN", "SW1A1AA", "UK")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "STREET LONDON ROAD 42",
                null,
                null,
                "SW1A1AA",
                "London",
                "LDN",
                "UK",
                null);

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);
    }

    @Test
    void Physical_Address_WithInternationalPostalCode_Belgium() {
        String correlationId = "physical-international-be-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "AVENUE", "DE CORTENBERG", "280", "Bruxelles", "BXL", "1040", "BE")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "AVENUE DE CORTENBERG 280",
                null,
                null,
                "1040",
                "Bruxelles",
                "BXL",
                "BE",
                null);

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);
    }

    @Test
    void PF_Physical_ANPR_WithMetricoCivicNumber() {
        String correlationId = "pf-physical-metrico-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "STRADA PROVINCIALE", "5", null, "5", null, null,
                null, null, null, null, "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA STRADA PROVINCIALE 5 KM 5",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    @Test
    void PF_Physical_ANPR_WithCivicNumberAndLetter() {
        String correlationId = "pf-physical-civic-letter-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "ROMA", "10", "A", null, null, null,
                null, null, null, null, "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA ROMA 10/A",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    @Test
    void PF_Physical_ANPR_WithSnCivic() {
        String correlationId = "pf-physical-sn-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "ROMA", "", null, null, "1", null,
                null, null, null, null, "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA ROMA SNC",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    @Test
    void PF_Physical_ANPR_WithColorAndScale() {
        String correlationId = "pf-physical-color-scale-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "MILANO", "25", null, null, null, "1",
                "A", null, null, null, "Milano", "MI", "20100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA MILANO 25",
                "ROSSO Scala A",
                null,
                "20100",
                "Milano",
                "MI",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    @Test
    void PF_Physical_ANPR_WithOnlyStreetAndNumber() {
        String correlationId = "pf-physical-minimal-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "ROMA", "10", "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA ROMA 10",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    @Test
    void PF_Physical_ANPR_WithComplexAddressStructure() {
        String correlationId = "pf-physical-complex-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "NAZIONALE DELLE COMUNICAZIONI", "100", null, null, null, "2",
                "B", "C", "Presso Palazzo Centrale", "Roma Capitale", "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA NAZIONALE DELLE COMUNICAZIONI 100",
                "NERO Scala B Scala est. C",
                "Presso Palazzo Centrale",
                "00100",
                "Roma",
                "RM",
                null,
                "Roma Capitale");

        verify(anprClient).callEService(any());
    }

    @Test
    void PF_Physical_ANPR_WithNumericSpecies() {
        String correlationId = "pf-physical-numeric-species-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "PIAZZA", "DELLA REPUBBLICA", "15", "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "PIAZZA DELLA REPUBBLICA 15",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    @Test
    void PG_Physical_RegistroImprese_NotFound() {
        AddressRegistroImprese addressRegistroImprese = new AddressRegistroImprese();
        addressRegistroImprese.setCf(VALID_PG_PIVA);
        addressRegistroImprese.setCode("ERR_00");
        addressRegistroImprese.setDescription("Sede not found");
        String correlationId = "pg-physical-not-found-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(addressRegistroImprese));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("REGISTRO_IMPRESE", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(), payload.getAddressType());
        assertNull(payload.getPhysicalAddress());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);

    }


    @Test
    void PG_Physical_RegistroImprese_error() {
        String correlationId = "pg-physical-technical-" + UUID.randomUUID();
        RuntimeException technicalError = new RuntimeException("Registro Imprese unavailable");
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertEquals(correlationId, response.getCorrelationId());
        assertSame(technicalError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);

    }

    private void assertPhysicalAddress(it.pagopa.pn.national.registries.model.CodeSqsDto payload,
                                       String address,
                                       String addressDetails,
                                       String at,
                                       String zip,
                                       String municipality,
                                       String province,
                                       String foreignState,
                                       String municipalityDetails) {
        assertNotNull(payload.getPhysicalAddress());
        assertEquals(address, payload.getPhysicalAddress().getAddress());
        if (addressDetails == null) {
            assertNull(payload.getPhysicalAddress().getAddressDetails());
        } else {
            assertEquals(addressDetails, payload.getPhysicalAddress().getAddressDetails());
        }
        if (at == null) {
            assertNull(payload.getPhysicalAddress().getAt());
        } else {
            assertEquals(at, payload.getPhysicalAddress().getAt());
        }
        assertEquals(zip, payload.getPhysicalAddress().getZip());
        assertEquals(municipality, payload.getPhysicalAddress().getMunicipality());
        if (province == null) {
            assertNull(payload.getPhysicalAddress().getProvince());
        } else {
            assertEquals(province, payload.getPhysicalAddress().getProvince());
        }
        if (foreignState == null) {
            assertNull(payload.getPhysicalAddress().getForeignState());
        } else {
            assertEquals(foreignState, payload.getPhysicalAddress().getForeignState());
        }
        if (municipalityDetails == null) {
            assertNull(payload.getPhysicalAddress().getMunicipalityDetails());
        } else {
            assertEquals(municipalityDetails, payload.getPhysicalAddress().getMunicipalityDetails());
        }
    }
}

