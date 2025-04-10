package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.DomicileType;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.entity.GatewayRequestTrackerEntity;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressesGatewayEvent;
import it.pagopa.pn.national.registries.model.*;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.repository.GatewayRequestTrackerRepository;
import it.pagopa.pn.national.registries.utils.CheckEmailUtils;
import it.pagopa.pn.national.registries.utils.CheckExceptionUtils;
import it.pagopa.pn.national.registries.utils.FeatureEnabledUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_CHECKING_CX_ID_FLAG;
import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static it.pagopa.pn.national.registries.constant.RecipientType.PG;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_ADDRESSES_REQUIRED;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_ADDRESSES_REQUIRED;
import static it.pagopa.pn.national.registries.utils.Utils.getMostRecentInstant;

@Service
@lombok.CustomLog
public class GatewayService extends GatewayConverter {
    private final AnprService anprService;
    private final InadService inadService;
    private final InfoCamereService infoCamereService;
    private final IpaService ipaService;
    private final SqsService sqsService;
    private final boolean pnNationalRegistriesCxIdFlag;
    private final FeatureEnabledUtils featureEnabledUtils;
    private final GatewayRequestTrackerRepository gatewayRequestTrackerRepository;

    private static final String CORRELATION_ID = "correlationId";
    private static final String AUDIT_LOG_START_CALL_MESSAGE = "Start searching physical address in the registry: {} for the request with correlationId: {} and recIndex: {}. Start request processing timestamp: {}";
    private static final String AUDIT_LOG_END_SUCCESS_MESSAGE = "The registry {} has responded successfully for the request with correlationId: {} and recIndex: {}. Response timestamp: {}";
    private static final String AUDIT_LOG_END_FAILURE_MESSAGE = "The registry {} has responded with an error for the request with correlationId: {} and recIndex: {}";
    private static final String SENT_TO_DLQ_INPUT_MESSAGE = "Sent to DLQ Input message for correlationId {} -> response: {}";


    public GatewayService(AnprService anprService,
                          InadService inadService,
                          InfoCamereService infoCamereService,
                          IpaService ipaService, SqsService sqsService,
                          FeatureEnabledUtils featureEnabledUtils,
                          @Value("${pn.national.registries.val.cx.id.enabled}") boolean pnNationalRegistriesCxIdFlag,
                          GatewayRequestTrackerRepository gatewayRequestTrackerRepository) {
        this.anprService = anprService;
        this.inadService = inadService;
        this.infoCamereService = infoCamereService;
        this.ipaService = ipaService;
        this.sqsService = sqsService;
        this.pnNationalRegistriesCxIdFlag = pnNationalRegistriesCxIdFlag;
        this.featureEnabledUtils = featureEnabledUtils;
        this.gatewayRequestTrackerRepository = gatewayRequestTrackerRepository;
    }

    public Mono<AddressOKDto> retrieveDigitalOrPhysicalAddressAsync(String recipientType, String pnNationalRegistriesCxId, AddressRequestBodyDto request) {
        checkFlagPnNationalRegistriesCxId(pnNationalRegistriesCxId);
        String correlationId = request.getFilter().getCorrelationId();
        sqsService.pushToInputQueue(InternalCodeSqsDto.builder()
                .taxId(request.getFilter().getTaxId())
                .correlationId(request.getFilter().getCorrelationId())
                .recipientType(recipientType)
                .domicileType(request.getFilter().getDomicileType().getValue())
                .referenceRequestDate(request.getFilter().getReferenceRequestDate())
                .pnNationalRegistriesCxId(pnNationalRegistriesCxId)
                .build(), pnNationalRegistriesCxId);

        return Mono.just(mapToAddressesOKDto(correlationId));
    }

    public Mono<AddressOKDto> retrievePhysicalAddresses(String pnNationalRegistriesCxId, PhysicalAddressesRequestBodyDto request) {
        String correlationId = request.getCorrelationId();
        if (request.getAddresses() == null || CollectionUtils.isEmpty(request.getAddresses())) {
            return Mono.error(new PnNationalRegistriesException("addresses required", HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, Charset.defaultCharset(), AddressErrorDto.class));
        }
        sqsService.pushToValidationInputQueue(MultiRecipientCodeSqsDto.builder()
                .correlationId(correlationId)
                .pnNationalRegistriesCxId(pnNationalRegistriesCxId)
                .referenceRequestDate(request.getReferenceRequestDate())
                .internalRecipientAdresses(mapToInternalRecipientAddresses(request.getAddresses()))
                .build(), pnNationalRegistriesCxId);
        return Mono.just(mapToAddressesOKDto(correlationId));
    }

    private List<MultiRecipientCodeSqsDto.InternalRecipientAddress> mapToInternalRecipientAddresses(List<RecipientAddressRequestBodyDto> recipients) {
        return recipients.stream().map(recipient -> MultiRecipientCodeSqsDto.InternalRecipientAddress.builder()
                .taxId(recipient.getFilter().getTaxId())
                .recipientType(recipient.getFilter().getRecipientType().getValue())
                .recIndex(Integer.valueOf(recipient.getFilter().getRecIndex()))
                .domicileType(DomicileType.PHYSICAL.name())
                .build()).toList();
    }

    public Mono<AddressOKDto> handleMessage(PnAddressGatewayEvent.Payload payload) {
        AddressRequestBodyDto addressRequestBodyDto = toAddressRequestBodyDto(payload);
        return retrieveDigitalOrPhysicalAddress(payload.getRecipientType(), payload.getPnNationalRegistriesCxId(), addressRequestBodyDto)
                .contextWrite(ctx -> enrichFluxContext(ctx, MDCUtils.retrieveMDCContextMap()));
    }

    private Context enrichFluxContext(Context ctx, Map<String, String> mdcCtx) {
        if (mdcCtx != null) {
            ctx = addToFluxContext(ctx, MDCUtils.MDC_TRACE_ID_KEY, mdcCtx.get(MDCUtils.MDC_TRACE_ID_KEY));
            ctx = addToFluxContext(ctx, MDCUtils.MDC_JTI_KEY, mdcCtx.get(MDCUtils.MDC_JTI_KEY));
            ctx = addToFluxContext(ctx, MDCUtils.MDC_PN_UID_KEY, mdcCtx.get(MDCUtils.MDC_PN_UID_KEY));
            ctx = addToFluxContext(ctx, MDCUtils.MDC_CX_ID_KEY, mdcCtx.get(MDCUtils.MDC_CX_ID_KEY));
            ctx = addToFluxContext(ctx, MDCUtils.MDC_PN_CX_TYPE_KEY, mdcCtx.get(MDCUtils.MDC_PN_CX_TYPE_KEY));
            ctx = addToFluxContext(ctx, MDCUtils.MDC_PN_CX_GROUPS_KEY, mdcCtx.get(MDCUtils.MDC_PN_CX_GROUPS_KEY));
            ctx = addToFluxContext(ctx, MDCUtils.MDC_PN_CX_ROLE_KEY, mdcCtx.get(MDCUtils.MDC_PN_CX_ROLE_KEY));
            ctx = addToFluxContext(ctx, MDCUtils.MDC_PN_CTX_MESSAGE_ID, mdcCtx.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
            ctx = addToFluxContext(ctx, CORRELATION_ID, mdcCtx.get(CORRELATION_ID));
        }
        return ctx;
    }

    private Context addToFluxContext(Context ctx, String key, String value) {
        if (value != null) {
            ctx = ctx.put(key, value);
        }
        return ctx;
    }

    private AddressRequestBodyDto toAddressRequestBodyDto(PnAddressGatewayEvent.Payload payload) {
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        AddressRequestBodyFilterDto addressRequestBodyFilterDto = new AddressRequestBodyFilterDto();
        addressRequestBodyFilterDto.setCorrelationId(payload.getCorrelationId());
        addressRequestBodyFilterDto.setReferenceRequestDate(payload.getReferenceRequestDate());
        addressRequestBodyFilterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.fromValue(payload.getDomicileType()));
        addressRequestBodyFilterDto.setTaxId(payload.getTaxId());
        addressRequestBodyDto.setFilter(addressRequestBodyFilterDto);
        return addressRequestBodyDto;
    }

    public Mono<AddressOKDto> retrieveDigitalOrPhysicalAddress(String recipientType, String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        log.info("recipientType {} and domicileType {}", recipientType, addressRequestBodyDto.getFilter().getDomicileType());
        RecipientType recipientTypeEnum = RecipientType.fromString(recipientType);
        return switch (recipientTypeEnum) {
            case PF -> retrieveAddressForPF(pnNationalRegistriesCxId, addressRequestBodyDto);
            case PG -> retrieveAddressForPG(pnNationalRegistriesCxId, addressRequestBodyDto);
        };
    }

    private Mono<GetAddressANPROKDto> getAddressANPR(GetAddressANPRRequestBodyDto requestBodyDto, String correlationId) {
        log.info("getAddressANPR for correlationId: {}", correlationId);
        return anprService.getAddressANPR(requestBodyDto);
    }

    private Mono<AddressOKDto> retrieveAddressForPF(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        String correlationId = addressRequestBodyDto.getFilter().getCorrelationId();
        if (AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.equals(addressRequestBodyDto.getFilter().getDomicileType())) {
            return getAddressANPR(convertToGetAddressAnprRequest(addressRequestBodyDto), correlationId)
                    .flatMap(anprResponse -> sqsService.pushToOutputQueue(anprToSqsDto(correlationId, anprResponse), pnNationalRegistriesCxId))
                    .doOnNext(sendMessageResponse -> log.info("retrieved physical address from ANPR for correlationId: {}", addressRequestBodyDto.getFilter().getCorrelationId()))
                    .doOnError(e -> logEServiceError(e, "can not retrieve physical address from ANPR: {}"))
                    .onErrorResume(e -> {
                        CodeSqsDto codeSqsDto = errorAnprToSqsDto(correlationId, e);
                        if(codeSqsDto != null) {
                            return sqsService.pushToOutputQueue(codeSqsDto, pnNationalRegistriesCxId);
                        }
                        return handleException(e, toInternalCodeSqsDto(addressRequestBodyDto.getFilter(), PF.name(), pnNationalRegistriesCxId));
                    })
                    .map(sendMessageResponse -> mapToAddressesOKDto(correlationId));
        } else {
            if (featureEnabledUtils.isPfNewWorkflowEnabled(addressRequestBodyDto.getFilter().getReferenceRequestDate().toInstant())) {
                return retrieveDigitalAddress(pnNationalRegistriesCxId, addressRequestBodyDto, correlationId, PF);
            }
            return inadService.callEService(convertToGetDigitalAddressInadRequest(addressRequestBodyDto), PF, null)
                    .flatMap(this::emailValidation)
                    .flatMap(inadResponse -> sqsService.pushToOutputQueue(inadToSqsDto(correlationId, inadResponse, DigitalAddressRecipientType.PERSONA_FISICA), pnNationalRegistriesCxId))
                    .doOnNext(sendMessageResponse -> log.info("retrieved digital address from INAD for correlationId: {}", addressRequestBodyDto.getFilter().getCorrelationId()))
                    .doOnError(e -> logEServiceError(e, "can not retrieve digital address from INAD: {}"))
                    .onErrorResume(e -> {
                        CodeSqsDto codeSqsDto = errorInadToSqsDto(correlationId, e);
                        if(codeSqsDto != null) {
                            return sqsService.pushToOutputQueue(codeSqsDto, pnNationalRegistriesCxId);
                        }
                        return handleException(e, toInternalCodeSqsDto(addressRequestBodyDto.getFilter(), PF.name(), pnNationalRegistriesCxId));
                    })
                    .map(sqs -> mapToAddressesOKDto(correlationId));
        }
    }

    private Mono<GetAddressRegistroImpreseOKDto> getRegistroImpreseLegalAddress(GetAddressRegistroImpreseRequestBodyDto requestBodyDto, String correlationId) {
        log.info("getRegistroImpreseLegalAddress for correlationId: {}", correlationId);
        return infoCamereService.getRegistroImpreseLegalAddress(requestBodyDto);
    }

    private Mono<AddressOKDto> retrieveAddressForPG(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        String correlationId = addressRequestBodyDto.getFilter().getCorrelationId();

        if (addressRequestBodyDto.getFilter().getDomicileType().equals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL)) {
            return getRegistroImpreseLegalAddress(convertToGetAddressRegistroImpreseRequest(addressRequestBodyDto), correlationId)
                    .flatMap(registroImpreseResponse -> sqsService.pushToOutputQueue(regImpToSqsDto(correlationId, registroImpreseResponse), pnNationalRegistriesCxId))
                    .doOnError(e -> logEServiceError(e, "can not retrieve physical address from Registro Imprese: {}"))
                    .onErrorResume(throwable -> handleException(throwable, toInternalCodeSqsDto(addressRequestBodyDto.getFilter(), PG.name(), pnNationalRegistriesCxId)))
                    .map(sendMessageResponse -> mapToAddressesOKDto(correlationId));
        } else {
            if(featureEnabledUtils.isPfNewWorkflowEnabled(addressRequestBodyDto.getFilter().getReferenceRequestDate().toInstant())) {
                return retrieveDigitalAddress(pnNationalRegistriesCxId, addressRequestBodyDto, correlationId, PG);
            }
            return retrieveOldDigitalAddress(pnNationalRegistriesCxId, addressRequestBodyDto, correlationId);
        }
    }

    private Mono<AddressOKDto> retrieveOldDigitalAddress(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto, String correlationId) {
        return ipaService.getIpaPec(convertToGetIpaPecRequest(addressRequestBodyDto))
                .flatMap(response -> {
                    if ((response.getDomicilioDigitale() == null &&
                            response.getDenominazione() == null &&
                            response.getCodEnte() == null &&
                            response.getTipo() == null) ||
                            !CheckEmailUtils.isValidEmail(response.getDomicilioDigitale())) {
                        return infoCamereService.getIniPecDigitalAddress(pnNationalRegistriesCxId, convertToGetDigitalAddressIniPecRequest(addressRequestBodyDto), addressRequestBodyDto.getFilter().getReferenceRequestDate());
                    }
                    log.info("retrieved digital address from IPA for correlationId: {}", addressRequestBodyDto.getFilter().getCorrelationId());
                    return sqsService.pushToOutputQueue(ipaToSqsDto(correlationId, response), pnNationalRegistriesCxId);
                })
                .doOnError(e -> logEServiceError(e, "can not retrieve digital address from IPA: {}"))
                .onErrorResume(e -> handleException(e, toInternalCodeSqsDto(addressRequestBodyDto.getFilter(), "PG", pnNationalRegistriesCxId)))
                .map(sqs -> mapToAddressesOKDto(correlationId));
    }

    @NotNull
    private Mono<AddressOKDto> retrieveDigitalAddress(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto, String correlationId, RecipientType recipientType) {
        AddressOKDto addressOKDto = mapToAddressesOKDto(correlationId);
        return infoCamereService.getIniPecDigitalAddress(pnNationalRegistriesCxId, convertToGetDigitalAddressIniPecRequest(addressRequestBodyDto), addressRequestBodyDto.getFilter().getReferenceRequestDate())
                .map(sqs -> addressOKDto)
                .doOnError(e -> logEServiceError(e, "can not save request on pn-batchRequest: {}"))
                .onErrorResume(e -> {
                    InternalCodeSqsDto internalCodeSqsDto = toInternalCodeSqsDto(addressRequestBodyDto.getFilter(), recipientType.name(), pnNationalRegistriesCxId);
                    return sqsService.pushToInputDlqQueue(internalCodeSqsDto, pnNationalRegistriesCxId)
                            .doOnNext(sendMessageResponse -> log.info(SENT_TO_DLQ_INPUT_MESSAGE,
                                    internalCodeSqsDto.getCorrelationId(),
                                    sendMessageResponse))
                            .thenReturn(addressOKDto);
                });
    }

    private InternalCodeSqsDto toInternalCodeSqsDto(AddressRequestBodyFilterDto filter, String recipientType, String pnNationalRegistriesCxId) {
        return InternalCodeSqsDto.builder()
                .taxId(filter.getTaxId())
                .correlationId(filter.getCorrelationId())
                .recipientType(recipientType)
                .domicileType(filter.getDomicileType().getValue())
                .referenceRequestDate(filter.getReferenceRequestDate())
                .pnNationalRegistriesCxId(pnNationalRegistriesCxId)
                .build();
    }

    private void checkFlagPnNationalRegistriesCxId(String pnNationalRegistriesCxId) {
        log.logChecking(PROCESS_CHECKING_CX_ID_FLAG);
        if (pnNationalRegistriesCxIdFlag && pnNationalRegistriesCxId == null) {
            log.logCheckingOutcome(PROCESS_CHECKING_CX_ID_FLAG, false, "pnNationalRegistriesCxId required");
            throw new PnNationalRegistriesException("pnNationalRegistriesCxId required", HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, Charset.defaultCharset(), AddressErrorDto.class);
        }
        log.logCheckingOutcome(PROCESS_CHECKING_CX_ID_FLAG, true);
    }

    private void logEServiceError(Throwable throwable, String message) {
        if (CheckExceptionUtils.isForLogLevelWarn(throwable)) {
            log.warn(message, throwable.getMessage());
        } else {
            log.error(message, throwable.getMessage());
        }
    }

    public Mono<SendMessageResponse> handleException(Throwable throwable, InternalCodeSqsDto internalCodeSqsDto) {
        if (throwable instanceof PnNationalRegistriesException exception && (exception.getStatusCode() == HttpStatus.BAD_REQUEST || exception.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)) {
            return sqsService.pushToInputDlqQueue(internalCodeSqsDto, internalCodeSqsDto.getPnNationalRegistriesCxId())
                    .doOnNext(sendMessageResponse -> log.info(SENT_TO_DLQ_INPUT_MESSAGE,
                            internalCodeSqsDto.getCorrelationId(),
                            sendMessageResponse));
        }
        return Mono.error(throwable);
    }

    public Mono<AddressOKDto> handleMessageMultiRequest(PnAddressesGatewayEvent.Payload payload) {
        List<AddressQueryRequest> addressQueryRequests = toAddressQueryRequests(payload);
        if (CollectionUtils.isEmpty(addressQueryRequests)) {
            return Mono.error(new PnInternalException(ERROR_MESSAGE_ADDRESSES_REQUIRED, ERROR_CODE_ADDRESSES_REQUIRED));
        }
        return retrieveMultiPhysicalAddress(addressQueryRequests)
                .contextWrite(ctx -> enrichFluxContext(ctx, MDCUtils.retrieveMDCContextMap()));
    }

    private Mono<AddressOKDto> retrieveMultiPhysicalAddress(List<AddressQueryRequest> addressQueryRequests) {
        String commonCorrelationId = addressQueryRequests.get(0).getCorrelationId();
        String commonPnNationalRegistriesCxId = addressQueryRequests.get(0).getPnNationalRegistriesCxId();
        log.info("RetrieveMultiPhysicalAddress for correlationId: {} and {} recipients", commonCorrelationId, addressQueryRequests.size());

        return gatewayRequestTrackerRepository.putIfAbsentOrRetrieve(commonCorrelationId)
            .flatMap(tracker -> retrieveMultiPhysicalAddress(commonCorrelationId, commonPnNationalRegistriesCxId, tracker, addressQueryRequests));
    }

    private Mono<AddressOKDto> retrieveMultiPhysicalAddress(String commonCorrelationId, String commonPnNationalRegistriesCxId, GatewayRequestTrackerEntity tracker, List<AddressQueryRequest> addressQueryRequests) {
        return Flux.fromIterable(addressQueryRequests)
                .flatMap(addressQueryRequest -> this.retrievePhysicalAddresses(addressQueryRequest, tracker.getRequestTimestamp()))
                .collectList()
                .doOnNext(addresses -> enrichAddressesWithResolutionTimes(tracker.getRequestTimestamp(), addresses))
                .map(addresses -> convertToMultiCodeSqsDto(addresses, commonCorrelationId))
                .flatMap(multiCodeSqsDto -> sqsService.pushMultiToOutputQueue(multiCodeSqsDto, commonPnNationalRegistriesCxId))
                .onErrorResume(t -> handleException(t, convertToMultiRecipientCodeSqsDto(addressQueryRequests)))
                .map(sendMessageResponse -> mapToAddressesOKDto(commonCorrelationId));
    }

    private Mono<MultiCodeSqsDto.PhysicalAddressSQSMessage> retrievePhysicalAddresses(AddressQueryRequest addressQueryRequest, Instant startResolutionTime) {
        return switch (addressQueryRequest.getRecipientType()) {
            case PF -> retrievePhysicalAddressForPF(addressQueryRequest, startResolutionTime);
            case PG -> retrievePhysicalAddressForPG(addressQueryRequest, startResolutionTime);
        };
    }

    private Mono<MultiCodeSqsDto.PhysicalAddressSQSMessage> retrievePhysicalAddressForPF(AddressQueryRequest addressQueryRequest, Instant startResolutionTime) {
        PnAuditLogEvent auditLogEvent = buildAndPrintRequestAuditLog(addressQueryRequest, GatewayDownstreamService.ANPR, startResolutionTime);

        return getAddressANPR(convertToGetAddressAnprRequest(addressQueryRequest), addressQueryRequest.getCorrelationId())
                .map(res -> convertAnprResponseToInternalRecipientAddress(res, addressQueryRequest))
                .onErrorResume(isAnprAddressNotFound, e -> {
                    log.info("correlationId: {} recIndex {} - ANPR - indirizzo non presente", addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex());
                    return Mono.just(anprNotFoundErrorToPhysicalAddressSQSMessage(addressQueryRequest));
                })
                .doOnNext(res -> auditLogEvent.generateSuccess(AUDIT_LOG_END_SUCCESS_MESSAGE, GatewayDownstreamService.ANPR, addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex(), res.getAddressResolutionEnd()).log())
                .doOnError(e -> auditLogEvent.generateFailure(AUDIT_LOG_END_FAILURE_MESSAGE, GatewayDownstreamService.ANPR, addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex(), e).log());
    }

    private static PnAuditLogEvent buildAndPrintRequestAuditLog(
            AddressQueryRequest addressQueryRequest,
            GatewayDownstreamService registry,
            Instant startResolutionTime
    ) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent auditLogEvent = auditLogBuilder.before(
            PnAuditLogEventType.AUD_NT_VALIDATION_ADDRESS_SEARCH,
            AUDIT_LOG_START_CALL_MESSAGE,
            registry,
            addressQueryRequest.getCorrelationId(),
            addressQueryRequest.getRecIndex(),
            startResolutionTime
        )
        .build();

        return auditLogEvent.log();
    }

    private final Predicate<Throwable> isAnprAddressNotFound = t -> t instanceof PnNationalRegistriesException exception
            && exception.getStatusCode() == HttpStatus.NOT_FOUND
            && StringUtils.hasText(exception.getResponseBodyAsString())
            && ANPR_CF_NOT_FOUND.matcher(exception.getResponseBodyAsString()).find();

    private Mono<MultiCodeSqsDto.PhysicalAddressSQSMessage> retrievePhysicalAddressForPG(AddressQueryRequest addressQueryRequest, Instant startResolutionTime) {
        PnAuditLogEvent auditLogEvent = buildAndPrintRequestAuditLog(addressQueryRequest, GatewayDownstreamService.REGISTRO_IMPRESE, startResolutionTime);
        return getRegistroImpreseLegalAddress(convertToGetAddressRegistroImpreseRequest(addressQueryRequest), addressQueryRequest.getCorrelationId())
                .map(res -> convertRegImprResponseToInternalRecipientAddress(res, addressQueryRequest))
                .doOnNext(res -> auditLogEvent.generateSuccess(AUDIT_LOG_END_SUCCESS_MESSAGE, GatewayDownstreamService.REGISTRO_IMPRESE, addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex(), res.getAddressResolutionEnd()).log())
                .doOnError(e -> auditLogEvent.generateFailure(AUDIT_LOG_END_FAILURE_MESSAGE, GatewayDownstreamService.REGISTRO_IMPRESE, addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex(), e).log());
    }

    private void enrichAddressesWithResolutionTimes(Instant startResolutionTime, List<MultiCodeSqsDto.PhysicalAddressSQSMessage> addresses) {
        Instant endResolutionTime = getMostRecentInstant(addresses.stream().map(MultiCodeSqsDto.PhysicalAddressSQSMessage::getAddressResolutionEnd).toList());
        log.debug("Enriching addresses with resolution times: startResolutionTime: {}, endResolutionTime: {}", startResolutionTime, endResolutionTime);
        addresses.forEach(address -> {
            address.setAddressResolutionStart(startResolutionTime);
            address.setAddressResolutionEnd(endResolutionTime);
        });
    }

    private Mono<SendMessageResponse> handleException(Throwable throwable, MultiRecipientCodeSqsDto multiRecipientCodeSqsDto) {
        if (throwable instanceof PnNationalRegistriesException exception && (exception.getStatusCode() == HttpStatus.BAD_REQUEST || exception.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)) {
            return sqsService.pushToInputDlqQueue(multiRecipientCodeSqsDto, multiRecipientCodeSqsDto.getPnNationalRegistriesCxId())
                    .doOnNext(sendMessageResponse -> log.info(SENT_TO_DLQ_INPUT_MESSAGE,
                            multiRecipientCodeSqsDto.getCorrelationId(),
                            sendMessageResponse));
        }
        return Mono.error(throwable);
    }

}
