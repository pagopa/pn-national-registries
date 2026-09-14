package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.nio.charset.Charset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    private static final String CF = "CF";
    private static final String C_ID = "correlationId";
    private static final String CX_ID = "clientId";

    @Mock
    private SqsService sqsService;

    @Mock
    private NationalRegistriesConfig nationalRegistriesConfig;

    @Mock
    private PhysicalAddressService physicalAddressService;

    @Mock
    private DigitalAddressService digitalAddressService;

    @Mock
    private GatewayUtils gatewayUtils;

    @InjectMocks
    private GatewayService gatewayService;


    @Test
    void retrieveDigitalOrPhysicalAddressAsync_shouldPushMessageToInputQueue() {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL
                );

        when(nationalRegistriesConfig.isValCxIdEnabled())
                .thenReturn(true);

        when(sqsService.pushToInputQueue(
                any(InternalCodeSqsDto.class),
                eq(CX_ID)
        )).thenReturn(
                Mono.just(SendMessageResponse.builder().build())
        );

        StepVerifier.create(
                        gatewayService.retrieveDigitalOrPhysicalAddressAsync(
                                "PF",
                                CX_ID,
                                request
                        )
                )
                .assertNext(result ->
                        assertEquals(C_ID, result.getCorrelationId())
                )
                .verifyComplete();

        verify(sqsService).pushToInputQueue(
                argThat(message ->
                        CF.equals(message.getTaxId())
                                && C_ID.equals(message.getCorrelationId())
                                && "PF".equals(message.getRecipientType())
                                && AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL
                                .getValue()
                                .equals(message.getDomicileType())
                                && CX_ID.equals(
                                message.getPnNationalRegistriesCxId()
                        )
                ),
                eq(CX_ID)
        );
    }


    @Test
    void retrieveDigitalOrPhysicalAddressAsync_shouldThrowWhenCxIdIsRequiredAndNull() {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL
                );

        when(nationalRegistriesConfig.isValCxIdEnabled())
                .thenReturn(true);

        PnNationalRegistriesException exception =
                assertThrows(
                        PnNationalRegistriesException.class,
                        () -> gatewayService.retrieveDigitalOrPhysicalAddressAsync(
                                "PF",
                                null,
                                request
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(sqsService);
    }


    @Test
    void retrieveDigitalOrPhysicalAddressAsync_shouldAcceptNullCxIdWhenValidationDisabled() {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL
                );

        when(nationalRegistriesConfig.isValCxIdEnabled())
                .thenReturn(false);

        when(sqsService.pushToInputQueue(
                any(InternalCodeSqsDto.class),
                isNull()
        )).thenReturn(
                Mono.just(SendMessageResponse.builder().build())
        );

        StepVerifier.create(
                        gatewayService.retrieveDigitalOrPhysicalAddressAsync(
                                "PF",
                                null,
                                request
                        )
                )
                .expectNextMatches(result ->
                        C_ID.equals(result.getCorrelationId())
                )
                .verifyComplete();
    }


    @Test
    void retrieveDigitalOrPhysicalAddress_shouldThrowForInvalidRecipientType() {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL
                );

        assertThrows(
                PnInternalException.class,
                () -> gatewayService.retrieveDigitalOrPhysicalAddress(
                        "INVALID",
                        CX_ID,
                        request
                )
        );
    }


    @Test
    void retrieveDigitalOrPhysicalAddress_shouldRetrievePhysicalAddressForPf() {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL
                );

        when(physicalAddressService.retrieveAsyncPhysicalAddressFromAnpr(
                CX_ID,
                request,
                C_ID
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        gatewayService.retrieveDigitalOrPhysicalAddress(
                                "PF",
                                CX_ID,
                                request
                        )
                )
                .assertNext(result ->
                        assertEquals(C_ID, result.getCorrelationId())
                )
                .verifyComplete();

        verify(physicalAddressService)
                .retrieveAsyncPhysicalAddressFromAnpr(
                        CX_ID,
                        request,
                        C_ID
                );

        verifyNoInteractions(digitalAddressService);
    }


    @Test
    void retrieveDigitalOrPhysicalAddress_shouldRetrievePhysicalAddressForPg() {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL
                );

        when(physicalAddressService
                .retrieveAsyncPhysicalAddressFromRegistroImprese(
                        CX_ID,
                        request,
                        C_ID
                ))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        gatewayService.retrieveDigitalOrPhysicalAddress(
                                "PG",
                                CX_ID,
                                request
                        )
                )
                .assertNext(result ->
                        assertEquals(C_ID, result.getCorrelationId())
                )
                .verifyComplete();

        verify(physicalAddressService)
                .retrieveAsyncPhysicalAddressFromRegistroImprese(
                        CX_ID,
                        request,
                        C_ID
                );

        verifyNoInteractions(digitalAddressService);
    }


    @Test
    void retrieveDigitalOrPhysicalAddress_shouldRetrieveDigitalAddressFromInadForPfWhenFallbackDisabled() {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL
                );

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(false);

        when(digitalAddressService.retrieveDigitalAddressFromInad(
                CX_ID,
                request,
                C_ID
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        gatewayService.retrieveDigitalOrPhysicalAddress(
                                "PF",
                                CX_ID,
                                request
                        )
                )
                .assertNext(result ->
                        assertEquals(C_ID, result.getCorrelationId())
                )
                .verifyComplete();

        verify(digitalAddressService)
                .retrieveDigitalAddressFromInad(
                        CX_ID,
                        request,
                        C_ID
                );

        verify(digitalAddressService, never())
                .retrieveDigitalAddressWithIniPecFallback(
                        any(),
                        any(),
                        any()
                );

        verifyNoInteractions(physicalAddressService);
    }


    @Test
    void retrieveDigitalOrPhysicalAddress_shouldUseIniPecFallbackForPfWhenEnabled() {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL
                );

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(true);

        when(digitalAddressService.retrieveDigitalAddressWithIniPecFallback(
                CX_ID,
                request,
                C_ID
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        gatewayService.retrieveDigitalOrPhysicalAddress(
                                "PF",
                                CX_ID,
                                request
                        )
                )
                .assertNext(result ->
                        assertEquals(C_ID, result.getCorrelationId())
                )
                .verifyComplete();

        verify(digitalAddressService)
                .retrieveDigitalAddressWithIniPecFallback(
                        CX_ID,
                        request,
                        C_ID
                );

        verify(digitalAddressService, never())
                .retrieveDigitalAddressFromInad(
                        any(),
                        any(),
                        any()
                );
    }


    @Test
    void retrieveDigitalOrPhysicalAddress_shouldRetrieveDigitalAddressForPg() {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL
                );

        when(digitalAddressService.retrieveDigitalAddressForPG(
                CX_ID,
                request,
                C_ID
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        gatewayService.retrieveDigitalOrPhysicalAddress(
                                "PG",
                                CX_ID,
                                request
                        )
                )
                .assertNext(result ->
                        assertEquals(C_ID, result.getCorrelationId())
                )
                .verifyComplete();

        verify(digitalAddressService)
                .retrieveDigitalAddressForPG(
                        CX_ID,
                        request,
                        C_ID
                );

        verifyNoInteractions(physicalAddressService);
    }


    @Test
    void retrievePhysicalAddressForPf_shouldPushNotFoundMessageToOutputQueue() {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL
                );

        byte[] body =
                "{\"codiceErroreAnomalia\":\"EN122\"}"
                        .getBytes(Charset.defaultCharset());

        PnNationalRegistriesException exception =
                new PnNationalRegistriesException(
                        "Not Found",
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        null,
                        body,
                        Charset.defaultCharset(),
                        null
                );

        when(physicalAddressService.retrieveAsyncPhysicalAddressFromAnpr(
                CX_ID,
                request,
                C_ID
        )).thenReturn(Mono.error(exception));

        when(sqsService.pushToOutputQueue(
                any(AddressSQSMessageDto.class),
                eq(CX_ID)
        )).thenReturn(
                Mono.just(SendMessageResponse.builder().build())
        );

        StepVerifier.create(
                        gatewayService.retrieveDigitalOrPhysicalAddress(
                                "PF",
                                CX_ID,
                                request
                        )
                )
                .assertNext(result ->
                        assertEquals(C_ID, result.getCorrelationId())
                )
                .verifyComplete();

        verify(sqsService).pushToOutputQueue(
                argThat(message ->
                        C_ID.equals(message.getCorrelationId())
                                && message.getPhysicalAddress() == null
                ),
                eq(CX_ID)
        );

        verify(sqsService, never())
                .pushToInputDlqQueue(any(), any());
    }


    @Test
    void retrieveDigitalAddressForPf_shouldPushNotFoundMessageToOutputQueueWhenFallbackDisabled() {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL
                );

        PnNationalRegistriesException exception =
                new PnNationalRegistriesException(
                        "CF non trovato",
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        null,
                        null,
                        Charset.defaultCharset(),
                        null
                );

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(false);

        when(digitalAddressService.retrieveDigitalAddressFromInad(
                CX_ID,
                request,
                C_ID
        )).thenReturn(Mono.error(exception));

        when(sqsService.pushToOutputQueue(
                any(AddressSQSMessageDto.class),
                eq(CX_ID)
        )).thenReturn(
                Mono.just(SendMessageResponse.builder().build())
        );

        StepVerifier.create(
                        gatewayService.retrieveDigitalOrPhysicalAddress(
                                "PF",
                                CX_ID,
                                request
                        )
                )
                .expectNextMatches(result ->
                        C_ID.equals(result.getCorrelationId())
                )
                .verifyComplete();

        verify(sqsService).pushToOutputQueue(
                any(AddressSQSMessageDto.class),
                eq(CX_ID)
        );
    }


    @ParameterizedTest
    @ValueSource(ints = {
            400,
            429
    })
    void retrieveAddressForPf_shouldSendEligibleErrorToDlq(int status) {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL
                );

        PnNationalRegistriesException exception =
                new PnNationalRegistriesException(
                        "error",
                        status,
                        "error",
                        null,
                        null,
                        Charset.defaultCharset(),
                        null
                );

        when(physicalAddressService.retrieveAsyncPhysicalAddressFromAnpr(
                CX_ID,
                request,
                C_ID
        )).thenReturn(Mono.error(exception));

        when(sqsService.pushToInputDlqQueue(
                any(InternalCodeSqsDto.class),
                eq(CX_ID)
        )).thenReturn(
                Mono.just(SendMessageResponse.builder().build())
        );

        StepVerifier.create(
                        gatewayService.retrieveDigitalOrPhysicalAddress(
                                "PF",
                                CX_ID,
                                request
                        )
                )
                .expectNextMatches(result ->
                        C_ID.equals(result.getCorrelationId())
                )
                .verifyComplete();

        verify(sqsService).pushToInputDlqQueue(
                argThat(message ->
                        CF.equals(message.getTaxId())
                                && C_ID.equals(message.getCorrelationId())
                                && "PF".equals(message.getRecipientType())
                                && CX_ID.equals(
                                message.getPnNationalRegistriesCxId()
                        )
                ),
                eq(CX_ID)
        );
    }


    @ParameterizedTest
    @ValueSource(ints = {
            400,
            429
    })
    void retrieveAddressForPg_shouldSendEligibleErrorToDlq(int status) {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL
                );

        PnNationalRegistriesException exception =
                new PnNationalRegistriesException(
                        "error",
                        status,
                        "error",
                        null,
                        null,
                        Charset.defaultCharset(),
                        null
                );

        when(physicalAddressService
                .retrieveAsyncPhysicalAddressFromRegistroImprese(
                        CX_ID,
                        request,
                        C_ID
                ))
                .thenReturn(Mono.error(exception));

        when(sqsService.pushToInputDlqQueue(
                any(InternalCodeSqsDto.class),
                eq(CX_ID)
        )).thenReturn(
                Mono.just(SendMessageResponse.builder().build())
        );

        StepVerifier.create(
                        gatewayService.retrieveDigitalOrPhysicalAddress(
                                "PG",
                                CX_ID,
                                request
                        )
                )
                .expectNextMatches(result ->
                        C_ID.equals(result.getCorrelationId())
                )
                .verifyComplete();

        verify(sqsService).pushToInputDlqQueue(
                any(InternalCodeSqsDto.class),
                eq(CX_ID)
        );
    }


    @Test
    void retrieveAddressForPf_shouldPropagateNonEligibleError() {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL
                );

        PnNationalRegistriesException exception =
                new PnNationalRegistriesException(
                        "error",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "error",
                        null,
                        null,
                        Charset.defaultCharset(),
                        null
                );

        when(physicalAddressService.retrieveAsyncPhysicalAddressFromAnpr(
                CX_ID,
                request,
                C_ID
        )).thenReturn(Mono.error(exception));

        StepVerifier.create(
                        gatewayService.retrieveDigitalOrPhysicalAddress(
                                "PF",
                                CX_ID,
                                request
                        )
                )
                .expectErrorMatches(error -> error == exception)
                .verify();

        verify(sqsService, never())
                .pushToInputDlqQueue(any(), any());

        verify(sqsService, never())
                .pushToOutputQueue(any(), any());
    }


    @Test
    void retrieveAddressForPg_shouldPropagateNonEligibleError() {
        AddressRequestBodyDto request =
                newAddressRequestBodyDto(
                        AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL
                );

        PnNationalRegistriesException exception =
                new PnNationalRegistriesException(
                        "error",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "error",
                        null,
                        null,
                        Charset.defaultCharset(),
                        null
                );

        when(physicalAddressService
                .retrieveAsyncPhysicalAddressFromRegistroImprese(
                        CX_ID,
                        request,
                        C_ID
                ))
                .thenReturn(Mono.error(exception));

        StepVerifier.create(
                        gatewayService.retrieveDigitalOrPhysicalAddress(
                                "PG",
                                CX_ID,
                                request
                        )
                )
                .expectErrorMatches(error -> error == exception)
                .verify();

        verify(sqsService, never())
                .pushToInputDlqQueue(any(), any());
    }


    @Test
    void handleExceptionAndSendToDlq_shouldSendBadRequestToDlq() {
        PnNationalRegistriesException exception =
                new PnNationalRegistriesException(
                        "bad request",
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        null,
                        null,
                        Charset.defaultCharset(),
                        null
                );

        InternalCodeSqsDto message =
                InternalCodeSqsDto.builder()
                        .taxId(CF)
                        .correlationId(C_ID)
                        .pnNationalRegistriesCxId(CX_ID)
                        .build();

        when(sqsService.pushToInputDlqQueue(message, CX_ID))
                .thenReturn(
                        Mono.just(SendMessageResponse.builder().build())
                );

        StepVerifier.create(
                        gatewayService.handleExceptionAndSendToDlq(
                                exception,
                                message
                        )
                )
                .verifyComplete();

        verify(sqsService)
                .pushToInputDlqQueue(message, CX_ID);
    }


    @Test
    void handleExceptionAndSendToDlq_shouldPropagateNotEligibleException() {
        RuntimeException exception =
                new RuntimeException("generic error");

        InternalCodeSqsDto message =
                InternalCodeSqsDto.builder()
                        .correlationId(C_ID)
                        .pnNationalRegistriesCxId(CX_ID)
                        .build();

        StepVerifier.create(
                        gatewayService.handleExceptionAndSendToDlq(
                                exception,
                                message
                        )
                )
                .expectErrorMatches(error -> error == exception)
                .verify();

        verifyNoInteractions(sqsService);
    }


    @Test
    void retrieveSyncPhysicalAddresses_shouldDelegateToPhysicalAddressService() {
        PhysicalAddressesRequestBodyDto request =
                new PhysicalAddressesRequestBodyDto();

        request.setCorrelationId(C_ID);
        request.setReferenceRequestDate(new Date());

        PhysicalAddressesResponseDto expected =
                new PhysicalAddressesResponseDto();
        expected.setCorrelationId(C_ID);

        when(physicalAddressService.retrieveSyncPhysicalAddresses(request))
                .thenReturn(Mono.just(expected));

        StepVerifier.create(
                        gatewayService.retrieveSyncPhysicalAddresses(request)
                )
                .expectNext(expected)
                .verifyComplete();

        verify(physicalAddressService)
                .retrieveSyncPhysicalAddresses(request);
    }


    @Test
    void handleMessage_shouldConvertPayloadAndProcessRequest() {
        Date referenceDate = new Date();

        PnAddressGatewayEvent.Payload payload =
                PnAddressGatewayEvent.Payload.builder()
                        .correlationId(C_ID)
                        .taxId(CF)
                        .pnNationalRegistriesCxId(CX_ID)
                        .recipientType("PF")
                        .domicileType("PHYSICAL")
                        .referenceRequestDate(referenceDate)
                        .build();

        when(physicalAddressService.retrieveAsyncPhysicalAddressFromAnpr(
                eq(CX_ID),
                any(AddressRequestBodyDto.class),
                eq(C_ID)
        )).thenReturn(Mono.empty());

        when(gatewayUtils.enrichFluxContext(
                any(Context.class),
                any()
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        StepVerifier.create(
                        gatewayService.handleMessage(payload)
                )
                .expectNextMatches(result ->
                        C_ID.equals(result.getCorrelationId())
                )
                .verifyComplete();

        verify(physicalAddressService)
                .retrieveAsyncPhysicalAddressFromAnpr(
                        eq(CX_ID),
                        argThat(request ->
                                request.getFilter() != null
                                        && C_ID.equals(
                                        request.getFilter()
                                                .getCorrelationId()
                                )
                                        && CF.equals(
                                        request.getFilter()
                                                .getTaxId()
                                )
                                        && AddressRequestBodyFilterDto
                                        .DomicileTypeEnum
                                        .PHYSICAL
                                        .equals(
                                                request.getFilter()
                                                        .getDomicileType()
                                        )
                        ),
                        eq(C_ID)
                );
    }


    private AddressRequestBodyDto newAddressRequestBodyDto(
            AddressRequestBodyFilterDto.DomicileTypeEnum domicileType
    ) {
        AddressRequestBodyFilterDto filter =
                new AddressRequestBodyFilterDto();

        filter.setTaxId(CF);
        filter.setCorrelationId(C_ID);
        filter.setDomicileType(domicileType);
        filter.setReferenceRequestDate(new Date());

        AddressRequestBodyDto request =
                new AddressRequestBodyDto();

        request.setFilter(filter);

        return request;
    }
}
