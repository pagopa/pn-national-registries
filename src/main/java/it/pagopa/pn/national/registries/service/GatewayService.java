package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressErrorDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.utils.CheckExceptionUtils;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.nio.charset.Charset;
import java.util.Map;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_CHECKING_CX_ID_FLAG;

@Service
@lombok.CustomLog
public class GatewayService extends GatewayConverter {

    private final AnprService anprService;
    private final InadService inadService;
    private final InfoCamereService infoCamereService;
    private final IpaService ipaService;
    private final SqsService sqsService;
    private final boolean pnNationalRegistriesCxIdFlag;

    public GatewayService(AnprService anprService,
                          InadService inadService,
                          InfoCamereService infoCamereService,
                          IpaService ipaService, SqsService sqsService,
                          @Value("${pn.national.registries.val.cx.id.enabled}") boolean pnNationalRegistriesCxIdFlag) {
        this.anprService = anprService;
        this.inadService = inadService;
        this.infoCamereService = infoCamereService;
        this.ipaService = ipaService;
        this.sqsService = sqsService;
        this.pnNationalRegistriesCxIdFlag = pnNationalRegistriesCxIdFlag;
    }

    public Mono<AddressOKDto> retrieveDigitalOrPhysicalAddressAsync(String recipientType, String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        checkFlagPnNationalRegistriesCxId(pnNationalRegistriesCxId);

        String correlationId = addressRequestBodyDto.getFilter().getCorrelationId();
        MDC.put("correlationId", correlationId);
        Map<String, String> copyOfContext = MDCUtils.retrieveMDCContextMap();

        Sinks.One<Tuple3<String, String, AddressRequestBodyDto>> sink = Sinks.one();

        sink.asMono()
                .flatMap(t -> retrieveDigitalOrPhysicalAddress(t.getT1(), t.getT2(), t.getT3()))
                .contextWrite(ctx -> enrichFluxContext(ctx, copyOfContext))
                .subscribe();

        var emitResult = sink.tryEmitValue(Tuples.of(recipientType, pnNationalRegistriesCxId != null ? pnNationalRegistriesCxId : "", addressRequestBodyDto));
        if (emitResult != Sinks.EmitResult.OK) {
            log.error("can not submit task: {}", emitResult);
            CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId);
            codeSqsDto.setError("can not submit task");
            sqsService.push(codeSqsDto, pnNationalRegistriesCxId)
                    .subscribe(ok -> {
                    }, e -> log.error("can not send message to SQS queue", e));
        }

        return Mono.just(mapToAddressesOKDto(correlationId));
    }

    public Mono<AddressOKDto> retrieveDigitalOrPhysicalAddress(String recipientType, String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        log.info("recipientType {} and domicileType {}", recipientType, addressRequestBodyDto.getFilter().getDomicileType());
        return switch (recipientType) {
            case "PF" -> retrievePhysicalAddress(pnNationalRegistriesCxId, addressRequestBodyDto);
            case "PG" -> retrieveDigitalAddress(pnNationalRegistriesCxId, addressRequestBodyDto);
            default -> {
                log.warn("recipientType {} is not valid", recipientType);
                throw new PnNationalRegistriesException("recipientType not valid", HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, Charset.defaultCharset(), AddressErrorDto.class);
            }
        };
    }

    private Mono<AddressOKDto> retrievePhysicalAddress(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        String correlationId = addressRequestBodyDto.getFilter().getCorrelationId();

        if (AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.equals(addressRequestBodyDto.getFilter().getDomicileType())) {
            return anprService.getAddressANPR(convertToGetAddressAnprRequest(addressRequestBodyDto))
                    .flatMap(anprResponse -> sqsService.push(anprToSqsDto(correlationId, anprResponse), pnNationalRegistriesCxId))
                    .doOnNext(sendMessageResponse -> log.info("retrieved physycal address from ANPR for correlationId: {} - cf: {}",addressRequestBodyDto.getFilter().getCorrelationId(),MaskDataUtils.maskString(addressRequestBodyDto.getFilter().getTaxId())))
                    .doOnError(e -> logEServiceError(e, "can not retrieve physical address from ANPR: {}"))
                    .onErrorResume(e -> sqsService.push(errorAnprToSqsDto(correlationId, e), pnNationalRegistriesCxId))
                    .map(sendMessageResponse -> mapToAddressesOKDto(correlationId));
        } else {
            return inadService.callEService(convertToGetDigitalAddressInadRequest(addressRequestBodyDto))
                    .flatMap(inadResponse -> sqsService.push(inadToSqsDto(correlationId, inadResponse), pnNationalRegistriesCxId))
                    .doOnNext(sendMessageResponse -> log.info("retrieved digital address from INAD for correlationId: {} - cf: {}",addressRequestBodyDto.getFilter().getCorrelationId(),MaskDataUtils.maskString(addressRequestBodyDto.getFilter().getTaxId())))
                    .doOnError(e -> logEServiceError(e, "can not retrieve digital address from INAD: {}"))
                    .onErrorResume(e -> sqsService.push(errorInadToSqsDto(correlationId, e), pnNationalRegistriesCxId))
                    .map(sqs -> mapToAddressesOKDto(correlationId));
        }
    }

    private Mono<AddressOKDto> retrieveDigitalAddress(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        String correlationId = addressRequestBodyDto.getFilter().getCorrelationId();

        if (addressRequestBodyDto.getFilter().getDomicileType().equals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL)) {
            return infoCamereService.getRegistroImpreseLegalAddress(convertToGetAddressRegistroImpreseRequest(addressRequestBodyDto))
                    .doOnNext(batchRequest -> log.info("Got registro imprese response for taxId: {}", MaskDataUtils.maskString(addressRequestBodyDto.getFilter().getTaxId())))
                    .flatMap(registroImpreseResponse -> sqsService.push(regImpToSqsDto(correlationId, registroImpreseResponse), pnNationalRegistriesCxId))
                    .doOnNext(sendMessageResponse -> log.info("retrieved physycal address from Registro Imprese for correlationId: {} - cf: {}",addressRequestBodyDto.getFilter().getCorrelationId(),MaskDataUtils.maskString(addressRequestBodyDto.getFilter().getTaxId())))
                    .doOnError(e -> logEServiceError(e, "can not retrieve physical address from Registro Imprese: {}"))
                    .onErrorResume(e -> sqsService.push(errorRegImpToSqsDto(correlationId, e), pnNationalRegistriesCxId))
                    .map(sendMessageResponse -> mapToAddressesOKDto(correlationId));
        } else {
            return ipaService.getIpaPec(convertToGetIpaPecRequest(addressRequestBodyDto))
                    .flatMap(response -> {
                        if (response.getDomicilioDigitale() == null &&
                                response.getDenominazione() == null &&
                                response.getCodEnte() == null &&
                                response.getTipo() == null) {
                            return infoCamereService.getIniPecDigitalAddress(pnNationalRegistriesCxId, convertToGetDigitalAddressIniPecRequest(addressRequestBodyDto));
                        }
                        log.info("retrieved digital address from IPA for correlationId: {} - cf: {}",addressRequestBodyDto.getFilter().getCorrelationId(),MaskDataUtils.maskString(addressRequestBodyDto.getFilter().getTaxId()));
                        return sqsService.push(ipaToSqsDto(correlationId, response), pnNationalRegistriesCxId);
                    })
                    .doOnError(e -> logEServiceError(e, "can not retrieve digital address from IPA: {}"))
                    .onErrorResume(e -> sqsService.push(errorIpaToSqsDto(correlationId, e), pnNationalRegistriesCxId))
                    .map(sqs -> mapToAddressesOKDto(correlationId));
        }
    }



    private void checkFlagPnNationalRegistriesCxId(String pnNationalRegistriesCxId) {
        log.logChecking(PROCESS_CHECKING_CX_ID_FLAG);
        if (pnNationalRegistriesCxIdFlag && pnNationalRegistriesCxId == null) {
            log.logCheckingOutcome(PROCESS_CHECKING_CX_ID_FLAG,false,"pnNationalRegistriesCxId required");
            throw new PnNationalRegistriesException("pnNationalRegistriesCxId required", HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, Charset.defaultCharset(), AddressErrorDto.class);
        }
        log.logCheckingOutcome(PROCESS_CHECKING_CX_ID_FLAG,true);
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
        }
        return ctx;
    }

    private Context addToFluxContext(Context ctx, String key, String value) {
        if (value != null) {
            ctx = ctx.put(key, value);
        }
        return ctx;
    }

    private void logEServiceError(Throwable throwable, String message) {
        if (CheckExceptionUtils.isForLogLevelWarn(throwable)) {
            log.warn(message, MaskDataUtils.maskInformation(throwable.getMessage()));
        } else {
            log.error(message, MaskDataUtils.maskInformation(throwable.getMessage()));
        }
    }
}
