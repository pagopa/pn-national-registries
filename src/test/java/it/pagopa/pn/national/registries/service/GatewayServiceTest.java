package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.model.AddressInternalResult;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    private static final String CF = "RSSMRA80A01H501U";
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

    @Mock
    private NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;

    @InjectMocks
    private GatewayService gatewayService;

    @Test
    void retrieveDigitalOrPhysicalAddressAsync_shouldPushMessageToInputQueue() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);

        when(nationalRegistriesConfig.isValCxIdEnabled()).thenReturn(true);
        when(sqsService.pushToInputQueue(any(InternalCodeSqsDto.class), eq(CX_ID)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddressAsync("PF", CX_ID, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(sqsService).pushToInputQueue(
                argThat(message ->
                        CF.equals(message.getTaxId())
                                && C_ID.equals(message.getCorrelationId())
                                && "PF".equals(message.getRecipientType())
                                && AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue().equals(message.getDomicileType())
                                && CX_ID.equals(message.getPnNationalRegistriesCxId())
                ),
                eq(CX_ID)
        );
    }

    @Test
    void retrieveDigitalOrPhysicalAddressAsync_shouldThrowWhenCxIdIsRequiredAndNull() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);
        when(nationalRegistriesConfig.isValCxIdEnabled()).thenReturn(true);

        PnNationalRegistriesException exception = assertThrows(
                PnNationalRegistriesException.class,
                () -> gatewayService.retrieveDigitalOrPhysicalAddressAsync("PF", null, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(sqsService);
    }

    @Test
    void retrieveDigitalOrPhysicalAddressAsync_shouldAllowNullCxIdWhenValidationIsDisabled() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);

        when(nationalRegistriesConfig.isValCxIdEnabled()).thenReturn(false);
        when(sqsService.pushToInputQueue(any(InternalCodeSqsDto.class), isNull()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddressAsync("PG", null, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(sqsService).pushToInputQueue(
                argThat(message ->
                        CF.equals(message.getTaxId())
                                && C_ID.equals(message.getCorrelationId())
                                && "PG".equals(message.getRecipientType())
                                && AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue().equals(message.getDomicileType())
                                && message.getPnNationalRegistriesCxId() == null
                ),
                isNull()
        );
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldPushPhysicalAddressForPf() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);

        when(physicalAddressService.retrieveAsyncPhysicalAddressFromAnpr(request, C_ID))
                .thenReturn(Mono.just(foundResult(GatewayDownstreamService.ANPR, true)));
        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), eq(CX_ID)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", CX_ID, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(physicalAddressService).retrieveAsyncPhysicalAddressFromAnpr(request, C_ID);
        verify(sqsService).pushToOutputQueue(argThat(message -> GatewayDownstreamService.ANPR.name().equals(message.getRegistry())), eq(CX_ID));
        verifyNoInteractions(digitalAddressService);
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldPushPhysicalAddressForPg() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);

        when(physicalAddressService.retrieveAsyncPhysicalAddressFromRegistroImprese(request, C_ID))
                .thenReturn(Mono.just(foundResult(GatewayDownstreamService.REGISTRO_IMPRESE, true)));
        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), eq(CX_ID)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PG", CX_ID, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(physicalAddressService).retrieveAsyncPhysicalAddressFromRegistroImprese(request, C_ID);
        verify(sqsService).pushToOutputQueue(argThat(message -> GatewayDownstreamService.REGISTRO_IMPRESE.name().equals(message.getRegistry())), eq(CX_ID));
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldPushInadAddressForPfWhenFound() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);

        when(nationalRegistriesConfig.isInadBatchEnabled()).thenReturn(false);
        when(digitalAddressService.retrieveDigitalAddressFromInadSync(request, C_ID, it.pagopa.pn.national.registries.constant.RecipientType.PF))
                .thenReturn(Mono.just(foundResult(GatewayDownstreamService.INAD, false)));
        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), eq(CX_ID)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", CX_ID, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(digitalAddressService).retrieveDigitalAddressFromInadSync(request, C_ID, it.pagopa.pn.national.registries.constant.RecipientType.PF);
        verify(sqsService).pushToOutputQueue(argThat(message -> GatewayDownstreamService.INAD.name().equals(message.getRegistry())), eq(CX_ID));
        verifyNoInteractions(nationalRegistriesRequestsRepository);
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldPushNotFoundInadMessageForPfWhenFallbackDisabled() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);

        when(nationalRegistriesConfig.isInadBatchEnabled()).thenReturn(false);
        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow()).thenReturn(false);
        when(digitalAddressService.retrieveDigitalAddressFromInadSync(request, C_ID, it.pagopa.pn.national.registries.constant.RecipientType.PF))
                .thenReturn(Mono.just(notFoundResult()));
        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), eq(CX_ID)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", CX_ID, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(sqsService).pushToOutputQueue(
                argThat(message -> GatewayDownstreamService.INAD.name().equals(message.getRegistry()) && message.getDigitalAddress().isEmpty()),
                eq(CX_ID)
        );
        verifyNoInteractions(nationalRegistriesRequestsRepository);
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldInitializeIniPecRequestForPfWhenFallbackEnabled() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        String expectedReferenceRequestDate = request.getFilter().getReferenceRequestDate().toString();

        when(nationalRegistriesConfig.isInadBatchEnabled()).thenReturn(false);
        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow()).thenReturn(true);
        when(digitalAddressService.retrieveDigitalAddressFromInadSync(request, C_ID, it.pagopa.pn.national.registries.constant.RecipientType.PF))
                .thenReturn(Mono.just(notFoundResult()));
        when(nationalRegistriesRequestsRepository.putRequest(any(NationalRegistriesRequest.class)))
                .thenAnswer(invocation -> Mono.just((NationalRegistriesRequest) invocation.getArgument(0)));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", CX_ID, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(nationalRegistriesRequestsRepository).putRequest(argThat(batchRequest ->
                C_ID.equals(batchRequest.getCorrelationId())
                        && expectedReferenceRequestDate.equals(batchRequest.getReferenceRequestDate())
                        && AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue().equals(batchRequest.getDomicileType())
                        && CF.equals(batchRequest.getTaxId())
                        && "PF".equals(batchRequest.getRecipientType())
                        && CX_ID.equals(batchRequest.getClientId())
                        && GatewayDownstreamService.INIPEC == batchRequest.getRegistry()
                        && RequestStatusEnum.NOT_WORKED == batchRequest.getStatus()
                        && "INIPEC#NOT_WORKED".equals(batchRequest.getRegistryStatus())
        ));
        verify(sqsService, never()).pushToOutputQueue(any(), any());
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldInitializeInadBatchRequestForPfWhenInadBatchEnabled() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);

        when(nationalRegistriesConfig.isInadBatchEnabled()).thenReturn(true);
        when(nationalRegistriesRequestsRepository.putRequest(any(NationalRegistriesRequest.class)))
                .thenAnswer(invocation -> Mono.just((NationalRegistriesRequest) invocation.getArgument(0)));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", CX_ID, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(nationalRegistriesRequestsRepository).putRequest(argThat(batchRequest ->
                GatewayDownstreamService.INAD == batchRequest.getRegistry()
                        && "PF".equals(batchRequest.getRecipientType())
                        && CX_ID.equals(batchRequest.getClientId())
                        && AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue().equals(batchRequest.getDomicileType())
        ));
        verifyNoInteractions(digitalAddressService);
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldConvertInadNotFoundErrorForPfWhenFallbackDisabled() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        PnNationalRegistriesException exception = new PnNationalRegistriesException(
                "CF non trovato",
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                null,
                null,
                Charset.defaultCharset(),
                null
        );

        when(nationalRegistriesConfig.isInadBatchEnabled()).thenReturn(false);
        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow()).thenReturn(false);
        when(digitalAddressService.retrieveDigitalAddressFromInadSync(request, C_ID, it.pagopa.pn.national.registries.constant.RecipientType.PF))
                .thenReturn(Mono.error(exception));
        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), eq(CX_ID)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", CX_ID, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(sqsService).pushToOutputQueue(
                argThat(message -> GatewayDownstreamService.INAD.name().equals(message.getRegistry()) && message.getDigitalAddress().isEmpty()),
                eq(CX_ID)
        );
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldNotFallbackToIniPecWhenPfInadFailsWithTechnicalError() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        RuntimeException technicalError = new RuntimeException("INAD unavailable");

        when(nationalRegistriesConfig.isInadBatchEnabled()).thenReturn(false);
        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow()).thenReturn(true);
        when(digitalAddressService.retrieveDigitalAddressFromInadSync(request, C_ID, it.pagopa.pn.national.registries.constant.RecipientType.PF))
                .thenReturn(Mono.error(technicalError));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", CX_ID, request))
                .expectErrorMatches(error -> error == technicalError)
                .verify();

        verify(nationalRegistriesRequestsRepository, never()).putRequest(any());
        verify(sqsService, never()).pushToOutputQueue(any(), any());
        verify(sqsService, never()).pushToInputDlqQueue(any(), any());
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldSendPfInadBadRequestToDlq() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        PnNationalRegistriesException exception = new PnNationalRegistriesException(
                "bad request",
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null,
                null,
                Charset.defaultCharset(),
                null
        );

        when(nationalRegistriesConfig.isInadBatchEnabled()).thenReturn(false);
        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow()).thenReturn(true);
        when(digitalAddressService.retrieveDigitalAddressFromInadSync(request, C_ID, it.pagopa.pn.national.registries.constant.RecipientType.PF))
                .thenReturn(Mono.error(exception));
        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CX_ID)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", CX_ID, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(sqsService).pushToInputDlqQueue(
                argThat(message ->
                        CF.equals(message.getTaxId())
                                && C_ID.equals(message.getCorrelationId())
                                && "PF".equals(message.getRecipientType())
                                && AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue().equals(message.getDomicileType())
                ),
                eq(CX_ID)
        );
        verify(nationalRegistriesRequestsRepository, never()).putRequest(any());
        verify(sqsService, never()).pushToOutputQueue(any(), any());
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldSendPfInadTooManyRequestsToDlq() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        PnNationalRegistriesException exception = new PnNationalRegistriesException(
                "too many requests",
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                null,
                null,
                Charset.defaultCharset(),
                null
        );

        when(nationalRegistriesConfig.isInadBatchEnabled()).thenReturn(false);
        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow()).thenReturn(true);
        when(digitalAddressService.retrieveDigitalAddressFromInadSync(request, C_ID, it.pagopa.pn.national.registries.constant.RecipientType.PF))
                .thenReturn(Mono.error(exception));
        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CX_ID)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", CX_ID, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(sqsService).pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CX_ID));
        verify(nationalRegistriesRequestsRepository, never()).putRequest(any());
        verify(sqsService, never()).pushToOutputQueue(any(), any());
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldPushIpaAddressForPgWhenFound() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);

        when(digitalAddressService.retrieveDigitalAddressFromIpa(request, C_ID))
                .thenReturn(Mono.just(foundResult(GatewayDownstreamService.IPA, false)));
        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), eq(CX_ID)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PG", CX_ID, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(digitalAddressService).retrieveDigitalAddressFromIpa(request, C_ID);
        verify(sqsService).pushToOutputQueue(argThat(message -> GatewayDownstreamService.IPA.name().equals(message.getRegistry())), eq(CX_ID));
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldInitializeIniPecRequestForPgWhenIpaDoesNotFindAddress() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);

        when(digitalAddressService.retrieveDigitalAddressFromIpa(request, C_ID))
                .thenReturn(Mono.just(new AddressInternalResult.NotFound()));
        when(nationalRegistriesRequestsRepository.putRequest(any(NationalRegistriesRequest.class)))
                .thenAnswer(invocation -> Mono.just((NationalRegistriesRequest) invocation.getArgument(0)));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PG", CX_ID, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(nationalRegistriesRequestsRepository).putRequest(argThat(batchRequest ->
                GatewayDownstreamService.INIPEC == batchRequest.getRegistry()
                        && "PG".equals(batchRequest.getRecipientType())
                        && AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue().equals(batchRequest.getDomicileType())
        ));
        verify(sqsService, never()).pushToOutputQueue(any(), any());
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldNotInitializeIniPecWhenPgIpaFailsWithTechnicalError() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        RuntimeException technicalError = new RuntimeException("IPA unavailable");

        when(digitalAddressService.retrieveDigitalAddressFromIpa(request, C_ID))
                .thenReturn(Mono.error(technicalError));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PG", CX_ID, request))
                .expectErrorMatches(error -> error == technicalError)
                .verify();

        verify(nationalRegistriesRequestsRepository, never()).putRequest(any());
        verify(sqsService, never()).pushToOutputQueue(any(), any());
        verify(sqsService, never()).pushToInputDlqQueue(any(), any());
    }

    @Test
    void retrieveDigitalOrPhysicalAddress_shouldPublishEmptyPfPhysicalResultWhenAnprReturnsRecognizedNotFound() {
        AddressRequestBodyDto request = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);
        PnNationalRegistriesException exception = new PnNationalRegistriesException(
                "not found",
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                null,
                "{\"codiceErroreAnomalia\":\"EN122\"}".getBytes(Charset.defaultCharset()),
                Charset.defaultCharset(),
                null
        );

        when(physicalAddressService.retrieveAsyncPhysicalAddressFromAnpr(request, C_ID))
                .thenReturn(Mono.error(exception));
        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), eq(CX_ID)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", CX_ID, request))
                .assertNext(result -> assertEquals(C_ID, result.getCorrelationId()))
                .verifyComplete();

        verify(sqsService).pushToOutputQueue(
                argThat(message ->
                        GatewayDownstreamService.ANPR.name().equals(message.getRegistry())
                                && AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue().equals(message.getAddressType())
                                && message.getPhysicalAddress() == null
                ),
                eq(CX_ID)
        );
        verifyNoInteractions(digitalAddressService);
    }

    @Test
    void handleExceptionAndSendToDlq_shouldSendBadRequestToDlq() {
        PnNationalRegistriesException exception = new PnNationalRegistriesException(
                "bad request",
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null,
                null,
                Charset.defaultCharset(),
                null
        );

        InternalCodeSqsDto message = InternalCodeSqsDto.builder()
                .taxId(CF)
                .correlationId(C_ID)
                .pnNationalRegistriesCxId(CX_ID)
                .build();

        when(sqsService.pushToInputDlqQueue(message, CX_ID)).thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.handleExceptionAndSendToDlq(exception, message))
                .verifyComplete();

        verify(sqsService).pushToInputDlqQueue(message, CX_ID);
    }

    @Test
    void retrieveSyncPhysicalAddresses_shouldDelegateToPhysicalAddressService() {
        PhysicalAddressesRequestBodyDto request = new PhysicalAddressesRequestBodyDto();
        request.setCorrelationId(C_ID);
        request.setReferenceRequestDate(new Date());

        PhysicalAddressesResponseDto expected = new PhysicalAddressesResponseDto();
        expected.setCorrelationId(C_ID);

        when(physicalAddressService.retrieveSyncPhysicalAddresses(request)).thenReturn(Mono.just(expected));

        StepVerifier.create(gatewayService.retrieveSyncPhysicalAddresses(request))
                .expectNext(expected)
                .verifyComplete();

        verify(physicalAddressService).retrieveSyncPhysicalAddresses(request);
    }

    @Test
    void handleMessage_shouldConvertPayloadAndProcessRequest() {
        Date referenceDate = new Date();
        PnAddressGatewayEvent.Payload payload = PnAddressGatewayEvent.Payload.builder()
                .correlationId(C_ID)
                .taxId(CF)
                .pnNationalRegistriesCxId(CX_ID)
                .recipientType("PF")
                .domicileType("PHYSICAL")
                .referenceRequestDate(referenceDate)
                .build();

        when(physicalAddressService.retrieveAsyncPhysicalAddressFromAnpr(any(AddressRequestBodyDto.class), eq(C_ID)))
                .thenReturn(Mono.just(foundResult(GatewayDownstreamService.ANPR, true)));
        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), eq(CX_ID)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));
        when(gatewayUtils.enrichFluxContext(any(Context.class), any())).thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(gatewayService.handleMessage(payload))
                .expectNextMatches(result -> C_ID.equals(result.getCorrelationId()))
                .verifyComplete();

        verify(physicalAddressService).retrieveAsyncPhysicalAddressFromAnpr(
                argThat((AddressRequestBodyDto body) ->
                        body.getFilter() != null
                                && C_ID.equals(body.getFilter().getCorrelationId())
                                && CF.equals(body.getFilter().getTaxId())
                                && AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL == body.getFilter().getDomicileType()
                ),
                eq(C_ID)
        );
    }

    private AddressInternalResult.Found foundResult(GatewayDownstreamService registry, boolean physical) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(C_ID);
        codeSqsDto.setRegistry(registry.name());
        codeSqsDto.setAddressType(physical
                ? AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue()
                : AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());

        if (physical) {
            PhysicalAddressDto physicalAddress = new PhysicalAddressDto();
            physicalAddress.setAddress("Via Roma 1");
            codeSqsDto.setPhysicalAddress(physicalAddress);
        } else {
            DigitalAddress digitalAddress = new DigitalAddress();
            digitalAddress.setAddress("address@pec.it");
            digitalAddress.setRecipient("PERSONALE");
            digitalAddress.setType("PEC");
            codeSqsDto.setDigitalAddress(Collections.singletonList(digitalAddress));
        }

        return new AddressInternalResult.Found(codeSqsDto);
    }

    private AddressInternalResult.NotFound notFoundResult() {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(C_ID);
        codeSqsDto.setRegistry(GatewayDownstreamService.INAD.name());
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
        codeSqsDto.setDigitalAddress(Collections.emptyList());
        return new AddressInternalResult.NotFound(codeSqsDto);
    }

    private AddressRequestBodyDto newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum domicileType) {
        AddressRequestBodyFilterDto filter = new AddressRequestBodyFilterDto();
        filter.setTaxId(CF);
        filter.setCorrelationId(C_ID);
        filter.setDomicileType(domicileType);
        filter.setReferenceRequestDate(new Date());

        AddressRequestBodyDto request = new AddressRequestBodyDto();
        request.setFilter(filter);
        return request;
    }
}
