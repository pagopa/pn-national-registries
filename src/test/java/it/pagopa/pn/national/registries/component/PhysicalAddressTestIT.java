package it.pagopa.pn.national.registries.component;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.AddressRegistroImprese;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest()
class PhysicalAddressTestIT extends AbstractNationalRegistriesComponentIT {

    @Test
    void PF_Physical_ANPR() {
        //migliora asserzioni sull'indirizzo
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
        assertNotNull(payload.getPhysicalAddress());
        assertNotNull(payload.getPhysicalAddress().getAddress());
        assertTrue(payload.getPhysicalAddress().getAddress().contains("ROMA"));
        assertEquals("Roma", payload.getPhysicalAddress().getMunicipality());
        assertEquals("RM", payload.getPhysicalAddress().getProvince());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(anprClient).callEService(any());
        verifyNoInteractions(inadClient, ipaClient);
    }

    @Test
    void PF_Physical_ANPR_NotFound() {
        String correlationId = "pf-physical-not-found-" + UUID.randomUUID();
        String responseBody = """
        {
          "codiceErroreAnomalia": "EN122"
        }
        """;

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
    }

    @Test
    //migliora asserzioni sull'indirizzo
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
        assertNotNull(payload.getPhysicalAddress());
        assertEquals("Milano", payload.getPhysicalAddress().getMunicipality());
        assertEquals("MI", payload.getPhysicalAddress().getProvince());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
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
    }
}

