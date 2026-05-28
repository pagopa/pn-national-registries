package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
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
    void removeInvalidEmailsFiltersInvalidEmails() {
        // given
        CodeSqsDto sqsDto = new CodeSqsDto();
        DigitalAddress valid = new DigitalAddress();
        valid.setAddress("valid@pec.it");
        DigitalAddress invalid = new DigitalAddress();
        invalid.setAddress("invalid_pec");

        sqsDto.setDigitalAddress(new ArrayList<>(List.of(valid, invalid)));

        // when
        DigitalAddressUtils.removeInvalidEmails(sqsDto);

        // then
        assertNotNull(sqsDto.getDigitalAddress());
        assertEquals(1, sqsDto.getDigitalAddress().size());
        assertEquals("valid@pec.it", sqsDto.getDigitalAddress().getFirst().getAddress());
    }

    @Test
    void removeInvalidEmailsHandlesNullDigitalAddressList() {
        // given
        CodeSqsDto sqsDto = new CodeSqsDto();
        sqsDto.setDigitalAddress(null);

        // when
        DigitalAddressUtils.removeInvalidEmails(sqsDto);

        // then
        assertNotNull(sqsDto.getDigitalAddress());
        assertTrue(sqsDto.getDigitalAddress().isEmpty());
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
                .buildErrorBatchRequest(status, error, request, LocalDateTime.now())
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
    void buildErrorBatchRequestReturnsNonNullMono() {
        BatchRequest request = new BatchRequest();

        when(infoCamereConverter.convertIniPecRequestToSqsDto(any(), anyString())).thenReturn(new CodeSqsDto());
        when(gatewayService.convertCodeSqsDtoToString(any())).thenReturn("msg");

        Mono<BatchRequest> errorMono = digitalAddressUtils.buildErrorBatchRequest(
                BatchStatus.ERROR, "err", request, LocalDateTime.now()
        );

        assertNotNull(errorMono);
    }

    @Test
    void removeInvalidEmailsFiltersListAndHandlesNullList() {
        // given: una lista con email valide e non valide
        CodeSqsDto sqsDto = new CodeSqsDto();
        DigitalAddress valid1 = new DigitalAddress();
        valid1.setAddress("a@pec.it");
        DigitalAddress invalid = new DigitalAddress();
        invalid.setAddress("not-an-email");
        DigitalAddress valid2 = new DigitalAddress();
        valid2.setAddress("b@pec.it");

        sqsDto.setDigitalAddress(new ArrayList<>(List.of(valid1, invalid, valid2)));

        // when
        DigitalAddressUtils.removeInvalidEmails(sqsDto);

        // then
        assertNotNull(sqsDto.getDigitalAddress());
        assertEquals(2, sqsDto.getDigitalAddress().size());
        assertEquals("a@pec.it", sqsDto.getDigitalAddress().get(0).getAddress());
        assertEquals("b@pec.it", sqsDto.getDigitalAddress().get(1).getAddress());

        // given: lista null
        CodeSqsDto sqsDtoWithNullList = new CodeSqsDto();
        sqsDtoWithNullList.setDigitalAddress(null);

        // when
        DigitalAddressUtils.removeInvalidEmails(sqsDtoWithNullList);

        // then
        assertNotNull(sqsDtoWithNullList.getDigitalAddress());
        assertTrue(sqsDtoWithNullList.getDigitalAddress().isEmpty());
    }

}