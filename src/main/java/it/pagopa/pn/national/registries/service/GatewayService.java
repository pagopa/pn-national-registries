package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.log.MDCWebFilter;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class GatewayService extends GatewayConverter {

    private final AnprService anprService;
    private final InadService inadService;
    private final InfoCamereService infoCamereService;
    private final SqsService sqsService;
    private final boolean pnNationalRegistriesCxIdFlag;

    public GatewayService(AnprService anprService,
                          InadService inadService,
                          InfoCamereService infoCamereService,
                          SqsService sqsService,
                          @Value("${pn.national.registries.val.cx.id.enabled}") boolean pnNationalRegistriesCxIdFlag) {
        this.anprService = anprService;
        this.inadService = inadService;
        this.infoCamereService = infoCamereService;
        this.sqsService = sqsService;
        this.pnNationalRegistriesCxIdFlag = pnNationalRegistriesCxIdFlag;
    }

    public Mono<AddressOKDto> retrieveDigitalOrPhysicalAddressAsync(String recipientType, String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        checkFlagPnNationalRegistriesCxId(pnNationalRegistriesCxId);

        String correlationId = addressRequestBodyDto.getFilter().getCorrelationId();
        String cf = addressRequestBodyDto.getFilter().getTaxId();

        Map<String, String> copyOfContext = MDCUtils.retrieveMDCContextMap();

        Sinks.One<Tuple3<String, String, AddressRequestBodyDto>> sink = Sinks.one();

        sink.asMono()
                .flatMap(t -> retrieveDigitalOrPhysicalAddress(t.getT1(), t.getT2(), t.getT3()))
                .contextWrite(ctx -> enrichFluxContext(ctx, copyOfContext))
                .subscribe();

        var emitResult = sink.tryEmitValue(Tuples.of(recipientType, pnNationalRegistriesCxId != null ? pnNationalRegistriesCxId : "", addressRequestBodyDto));
        if (emitResult != Sinks.EmitResult.OK) {
            log.error("can not submit task: {}", emitResult);
            CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId, cf);
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
        String cf = addressRequestBodyDto.getFilter().getTaxId();

        if (AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.equals(addressRequestBodyDto.getFilter().getDomicileType())) {
            return anprService.getAddressANPR(convertToGetAddressAnprRequest(addressRequestBodyDto))
                    .flatMap(anprResponse -> sqsService.push(anprToSqsDto(correlationId, cf, anprResponse), pnNationalRegistriesCxId))
                    .doOnError(e -> log.error("can not retrieve physical address from ANPR", e))
                    .onErrorResume(e -> sqsService.push(errorAnprToSqsDto(correlationId, cf, e), pnNationalRegistriesCxId))
                    .map(sendMessageResponse -> mapToAddressesOKDto(correlationId));
        } else {
            return inadService.callEService(convertToGetDigitalAddressInadRequest(addressRequestBodyDto))
                    .flatMap(inadResponse -> sqsService.push(inadToSqsDto(correlationId, cf, inadResponse), pnNationalRegistriesCxId))
                    .doOnError(e -> log.error("can not retrieve digital address from INAD", e))
                    .onErrorResume(e -> sqsService.push(errorInadToSqsDto(correlationId, cf, e), pnNationalRegistriesCxId))
                    .map(sqs -> mapToAddressesOKDto(correlationId));
        }
    }

    private Mono<AddressOKDto> retrieveDigitalAddress(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        String correlationId = addressRequestBodyDto.getFilter().getCorrelationId();
        String cf = addressRequestBodyDto.getFilter().getTaxId();

        if (addressRequestBodyDto.getFilter().getDomicileType().equals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL)) {
            return infoCamereService.getRegistroImpreseLegalAddress(convertToGetAddressRegistroImpreseRequest(addressRequestBodyDto))
                    .flatMap(registroImpreseResponse -> sqsService.push(regImpToSqsDto(correlationId, cf, registroImpreseResponse), pnNationalRegistriesCxId))
                    .doOnError(e -> log.error("can not retrieve physical address from Registro Imprese", e))
                    .onErrorResume(e -> sqsService.push(errorRegImpToSqsDto(correlationId, cf, e), pnNationalRegistriesCxId))
                    .map(sendMessageResponse -> mapToAddressesOKDto(correlationId));
        } else {
            return infoCamereService.getIniPecDigitalAddress(pnNationalRegistriesCxId, convertToGetDigitalAddressIniPecRequest(addressRequestBodyDto))
                    .map(iniPecResponse -> mapToAddressesOKDto(correlationId));
        }
    }

    private void checkFlagPnNationalRegistriesCxId(String pnNationalRegistriesCxId) {
        if (pnNationalRegistriesCxIdFlag && pnNationalRegistriesCxId == null) {
            throw new PnNationalRegistriesException("pnNationalRegistriesCxId required", HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, Charset.defaultCharset(), AddressErrorDto.class);
        }
    }

    private Context enrichFluxContext(Context ctx, Map<String, String> mdcCtx) {
        if (mdcCtx != null) {
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_TRACE_ID_KEY, mdcCtx.get(MDCWebFilter.MDC_TRACE_ID_KEY));
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_JTI_KEY, mdcCtx.get(MDCWebFilter.MDC_JTI_KEY));
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_PN_UID_KEY, mdcCtx.get(MDCWebFilter.MDC_PN_UID_KEY));
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_CX_ID_KEY, mdcCtx.get(MDCWebFilter.MDC_CX_ID_KEY));
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_PN_CX_TYPE_KEY, mdcCtx.get(MDCWebFilter.MDC_PN_CX_TYPE_KEY));
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_PN_CX_GROUPS_KEY, mdcCtx.get(MDCWebFilter.MDC_PN_CX_GROUPS_KEY));
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_PN_CX_ROLE_KEY, mdcCtx.get(MDCWebFilter.MDC_PN_CX_ROLE_KEY));
        }
        return ctx;
    }

    private Context addToFluxContext(Context ctx, String key, String value) {
        if (value != null) {
            ctx = ctx.put(key, value);
        }
        return ctx;
    }
}
