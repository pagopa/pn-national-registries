package it.pagopa.pn.national.registries.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.constant.GatewayError;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.PhysicalAddressResponseDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.AddressQueryRequest;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.util.context.Context;

import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayUtils {

    private final String CORRELATION_ID = "correlationId";
    private final String AUDIT_LOG_START_CALL_MESSAGE = "Start searching physical address in the registry: {} for the request with correlationId: {} and recIndex: {}";

    private final ObjectMapper mapper;

    private static final int CF_LENGTH = 16;


    public String convertCodeSqsDtoToString(CodeSqsDto codeSqsDto) {
        try {
            return mapper.writeValueAsString(codeSqsDto);
        } catch (JsonProcessingException e) {
            throw new DigitalAddressException("can not convert SQS DTO to String", e);
        }
    }

    public RecipientType retrieveRecipientType(String cf, String recipientType) {
        if(Objects.nonNull(recipientType)){
            return RecipientType.fromString(recipientType);
        }
        return StringUtils.hasText(cf) && cf.length() == CF_LENGTH ? RecipientType.PF : RecipientType.PG;
    }

    public Context enrichFluxContext(Context ctx, Map<String, String> mdcCtx) {
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

    public PnAuditLogEvent buildAndPrintRequestAuditLog(AddressQueryRequest addressQueryRequest, GatewayDownstreamService registry) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent auditLogEvent = auditLogBuilder.before(
                        PnAuditLogEventType.AUD_NT_VALIDATION_ADDRESS_SEARCH,
                        AUDIT_LOG_START_CALL_MESSAGE,
                        registry,
                        addressQueryRequest.getCorrelationId(),
                        addressQueryRequest.getRecIndex()
                )
                .build();

        return auditLogEvent.log();
    }

    public PhysicalAddressResponseDto handleException(Throwable throwable, AddressQueryRequest addressQueryRequest, GatewayDownstreamService gatewayDownstreamService) {
        PhysicalAddressResponseDto addressInfo = new PhysicalAddressResponseDto();
        addressInfo.setRecIndex(addressQueryRequest.getRecIndex());
        addressInfo.setRegistry(gatewayDownstreamService.name());
        addressInfo.setError(toAddressResponseError(throwable).name());
        addressInfo.setErrorStatus(toAddressResponseErrorStatus(throwable).value());
        return addressInfo;
    }

    private GatewayError toAddressResponseError(Throwable throwable) {
        if(throwable instanceof PnNationalRegistriesException exception && exception.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            return GatewayError.DOWNSTREAM_TOO_MANY_REQUESTS;
        }
        return GatewayError.DOWNSTREAM_REQUEST_ERROR;
    }

    private HttpStatus toAddressResponseErrorStatus(Throwable throwable) {
        if(throwable instanceof PnNationalRegistriesException exception) {
            return HttpStatus.valueOf(exception.getStatusCode().value());
        }

        //Default case
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public void logEServiceError(Throwable throwable, String message) {
        if (CheckExceptionUtils.isForLogLevelWarn(throwable)) {
            log.warn(message, throwable.getMessage());
        } else {
            log.error(message, throwable.getMessage());
        }
    }
}
