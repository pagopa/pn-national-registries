package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.EService;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.service.GatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DigitalAddressUtilsTest {
    private InfoCamereConverter infoCamereConverter;
    private GatewayService gatewayService;
    private DigitalAddressUtils digitalAddressUtils;

    @BeforeEach
    void setUp() throws Exception {
        infoCamereConverter = mock(InfoCamereConverter.class);
        gatewayService = mock(GatewayService.class);

        Constructor<DigitalAddressUtils> constructor =
                DigitalAddressUtils.class.getDeclaredConstructor(InfoCamereConverter.class, GatewayService.class);
        constructor.setAccessible(true);
        digitalAddressUtils = constructor.newInstance(infoCamereConverter, gatewayService);
    }

    @Test
    void updateBatchRequestFieldsFiltersInvalidEmailsAndSetsFields() {
        BatchRequest request = new BatchRequest();
        BatchStatus status = BatchStatus.WORKED;
        LocalDateTime now = LocalDateTime.now();
        Pec pec = new Pec();

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        DigitalAddress valid = new DigitalAddress();
        valid.setAddress("valid@pec.it");
        DigitalAddress invalid = new DigitalAddress();
        invalid.setAddress("invalid_pec");
        codeSqsDto.setDigitalAddress(new ArrayList<>(List.of(valid, invalid)));

        when(infoCamereConverter.convertResponsePecToCodeSqsDto(request, pec)).thenReturn(codeSqsDto);
        when(gatewayService.convertCodeSqsDtoToString(any(CodeSqsDto.class))).thenReturn("serialized-message");

        BatchRequest result = digitalAddressUtils
                .updateBatchRequestFields(request, status, now, pec)
                .block();

        assertNotNull(result);
        assertSame(request, result);
        assertEquals("serialized-message", result.getMessage());
        assertEquals(EService.INIPEC.name(), result.getEservice());
        assertEquals(BatchStatus.WORKED.getValue(), result.getStatus());
        assertEquals(BatchSendStatus.NOT_SENT.getValue(), result.getSendStatus());
        assertEquals(now, result.getLastReserved());

        assertNotNull(codeSqsDto.getDigitalAddress());
        assertEquals(1, codeSqsDto.getDigitalAddress().size());
        assertEquals("valid@pec.it", codeSqsDto.getDigitalAddress().getFirst().getAddress());

        verify(infoCamereConverter, times(1)).convertResponsePecToCodeSqsDto(request, pec);
        verify(gatewayService, times(1)).convertCodeSqsDtoToString(codeSqsDto);
    }

    @Test
    void updateBatchRequestFieldsHandlesNullDigitalAddressList() {
        BatchRequest request = new BatchRequest();
        BatchStatus status = BatchStatus.WORKING;
        LocalDateTime now = LocalDateTime.now();
        Pec pec = new Pec();

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setDigitalAddress(null);

        when(infoCamereConverter.convertResponsePecToCodeSqsDto(request, pec)).thenReturn(codeSqsDto);
        when(gatewayService.convertCodeSqsDtoToString(any(CodeSqsDto.class))).thenReturn("serialized-message");

        BatchRequest result = digitalAddressUtils
                .updateBatchRequestFields(request, status, now, pec)
                .block();

        assertNotNull(result);
        assertSame(request, result);
        assertEquals("serialized-message", result.getMessage());
        assertEquals(EService.INIPEC.name(), result.getEservice());
        assertEquals(BatchStatus.WORKING.getValue(), result.getStatus());
        assertEquals(BatchSendStatus.NOT_SENT.getValue(), result.getSendStatus());
        assertEquals(now, result.getLastReserved());

        assertNotNull(codeSqsDto.getDigitalAddress());
        assertTrue(codeSqsDto.getDigitalAddress().isEmpty());

        verify(infoCamereConverter, times(1)).convertResponsePecToCodeSqsDto(request, pec);
        verify(gatewayService, times(1)).convertCodeSqsDtoToString(codeSqsDto);
    }

    @Test
    void buildErrorBatchRequestSetsFields() {
        BatchRequest request = new BatchRequest();
        BatchStatus status = BatchStatus.ERROR;
        String error = "generic error";

        CodeSqsDto sqsDto = new CodeSqsDto();
        when(infoCamereConverter.convertIniPecRequestToSqsDto(request, error)).thenReturn(sqsDto);
        when(gatewayService.convertCodeSqsDtoToString(sqsDto)).thenReturn("error-message");

        BatchRequest result = digitalAddressUtils
                .buildErrorBatchRequest(status, error, request)
                .block();

        assertNotNull(result);
        assertSame(request, result);
        assertEquals("error-message", result.getMessage());
        assertEquals(EService.INIPEC.name(), result.getEservice());
        assertEquals(BatchStatus.ERROR.getValue(), result.getStatus());

        verify(infoCamereConverter, times(1)).convertIniPecRequestToSqsDto(request, error);
        verify(gatewayService, times(1)).convertCodeSqsDtoToString(sqsDto);
    }

    @Test
    void methodsReturnNonNullMono() {
        BatchRequest request = new BatchRequest();
        Pec pec = new Pec();

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setDigitalAddress(new ArrayList<>());

        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(), any())).thenReturn(codeSqsDto);
        when(infoCamereConverter.convertIniPecRequestToSqsDto(any(), anyString())).thenReturn(new CodeSqsDto());
        when(gatewayService.convertCodeSqsDtoToString(any())).thenReturn("msg");

        Mono<BatchRequest> updateMono = digitalAddressUtils.updateBatchRequestFields(
                request, BatchStatus.WORKED, LocalDateTime.now(), pec
        );
        Mono<BatchRequest> errorMono = digitalAddressUtils.buildErrorBatchRequest(
                BatchStatus.ERROR, "err", request
        );

        assertNotNull(updateMono);
        assertNotNull(errorMono);
    }

}