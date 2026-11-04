package it.pagopa.pn.national.registries.integration.physical;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressErrorDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.integration.AbstractNationalRegistriesComponentIT;
import it.pagopa.pn.national.registries.integration.PnNationalRegistriesTestApplication;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.service.SqsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
class PhysicalAddressTestIT extends AbstractNationalRegistriesComponentIT {

    @MockitoSpyBean
    private SqsService sqsService;

    @Test
    void PF_Physical_OutputQueue_OK() {
        String correlationId = "pf-output-ok-" + UUID.randomUUID();

        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(VALID_PF_CF, "VIA", "ROMA", "10", "Roma", "RM", "00100")));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);

        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);

        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());

        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("ANPR", payload.getRegistry());

        ArgumentCaptor<CodeSqsDto> captor = ArgumentCaptor.forClass(CodeSqsDto.class);

        verify(sqsService).pushToOutputQueue(captor.capture(), any());

        assertEquals(correlationId, captor.getValue().getCorrelationId());

        verify(sqsService, never()).pushToInputDlqQueue(any(), any());
    }

    @Test
    void PF_Physical_OutputQueue_KO() {
        String correlationId = "pf-output-ko-" + UUID.randomUUID();
        RuntimeException sqsError = new RuntimeException("Output queue unavailable");

        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(VALID_PF_CF, "VIA", "ROMA", "10", "Roma", "RM", "00100")));

        doReturn(Mono.error(sqsError)).when(sqsService).pushToOutputQueue(any(CodeSqsDto.class), any());

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);

        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertEquals(correlationId, response.getCorrelationId());
        assertSame(sqsError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        ArgumentCaptor<CodeSqsDto> captor = ArgumentCaptor.forClass(CodeSqsDto.class);

        verify(sqsService).pushToOutputQueue(captor.capture(), any());

        assertEquals(correlationId, captor.getValue().getCorrelationId());

        verify(sqsService, never()).pushToInputDlqQueue(any(), any());
    }

    @Test
    void PG_Physical_DLQ_OK() {
        String correlationId = "pg-dlq-ok-" + UUID.randomUUID();

        PnNationalRegistriesException downstreamError = new PnNationalRegistriesException(
                "Bad request",
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null,
                null,
                StandardCharsets.UTF_8,
                AddressErrorDto.class
        );

        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.error(downstreamError));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);

        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        Message dlqMessage = receiveSingleMessage(INPUT_DLQ_QUEUE);

        assertNotNull(dlqMessage);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));

        ArgumentCaptor<InternalCodeSqsDto> captor = ArgumentCaptor.forClass(InternalCodeSqsDto.class);

        verify(sqsService).pushToInputDlqQueue(captor.capture(), any());

        InternalCodeSqsDto payload = captor.getValue();

        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals(VALID_PG_PIVA, payload.getTaxId());
        assertEquals("PG", payload.getRecipientType());
        assertEquals("PHYSICAL", payload.getDomicileType());

        verify(sqsService, never()).pushToOutputQueue(any(), any());
    }

    @Test
    void PG_Physical_DLQ_KO() {
        String correlationId = "pg-dlq-ko-" + UUID.randomUUID();
        RuntimeException sqsError = new RuntimeException("DLQ unavailable");

        PnNationalRegistriesException downstreamError = new PnNationalRegistriesException(
                "Bad request",
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null,
                null,
                StandardCharsets.UTF_8,
                AddressErrorDto.class
        );

        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.error(downstreamError));

        doReturn(Mono.error(sqsError)).when(sqsService).pushToInputDlqQueue(any(InternalCodeSqsDto.class), any());

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);

        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertEquals(correlationId, response.getCorrelationId());
        assertSame(sqsError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));

        ArgumentCaptor<InternalCodeSqsDto> captor = ArgumentCaptor.forClass(InternalCodeSqsDto.class);

        verify(sqsService).pushToInputDlqQueue(captor.capture(), any());

        InternalCodeSqsDto payload = captor.getValue();

        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals(VALID_PG_PIVA, payload.getTaxId());
        assertEquals("PG", payload.getRecipientType());
        assertEquals("PHYSICAL", payload.getDomicileType());

        verify(sqsService, never()).pushToOutputQueue(any(), any());
    }
}