package it.pagopa.pn.national.registries.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.constant.GatewayError;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.PhysicalAddressResponseDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.AddressQueryRequest;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.util.context.Context;

import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static it.pagopa.pn.national.registries.constant.RecipientType.PG;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GatewayUtilsTest {

    private static final String CORRELATION_ID = "correlationId";
    private static final String VALID_CF = "MNZVMH95B09L084U";

    private ObjectMapper mapper;
    private GatewayUtils gatewayUtils;

    @BeforeEach
    void setUp() {
        mapper = mock(ObjectMapper.class);
        gatewayUtils = new GatewayUtils(mapper);
    }

    @Test
    void retrieveRecipientTypeShouldReturnPfWhenCfHas16Characters() {
        RecipientType result = gatewayUtils.retrieveRecipientType(VALID_CF, null);
        assertEquals(PF, result);
    }


    @Test
    void retrieveRecipientTypeShouldReturnPfWhenCfHas16CharactersButRecipientTypeIsPG() {
        RecipientType result = gatewayUtils.retrieveRecipientType(VALID_CF, PG.name());
        assertEquals(PG, result);
    }

    @Test
    void retrieveRecipientTypeShouldReturnPgWhenCfIsNotPresent() {
        RecipientType result = gatewayUtils.retrieveRecipientType(null, null);
        assertEquals(PG, result);
    }

    @Test
    void retrieveRecipientTypeShouldReturnPgWhenCfHasWrongLength() {
        RecipientType result = gatewayUtils.retrieveRecipientType("12345678901", null);
        assertEquals(PG, result);
    }


    @Test
    void convertCodeSqsDtoToString_shouldReturnSerializedDto() throws Exception {
        CodeSqsDto dto = new CodeSqsDto();

        when(mapper.writeValueAsString(dto))
                .thenReturn("{\"correlationId\":\"123\"}");

        String result = gatewayUtils.convertCodeSqsDtoToString(dto);

        assertEquals("{\"correlationId\":\"123\"}", result);

        verify(mapper).writeValueAsString(dto);
    }

    @Test
    void convertCodeSqsDtoToString_shouldThrowDigitalAddressExceptionWhenSerializationFails()
            throws Exception {

        CodeSqsDto dto = new CodeSqsDto();

        JsonProcessingException jsonException =
                new JsonProcessingException("serialization error") {};

        when(mapper.writeValueAsString(dto))
                .thenThrow(jsonException);

        DigitalAddressException exception = assertThrows(
                DigitalAddressException.class,
                () -> gatewayUtils.convertCodeSqsDtoToString(dto)
        );

        assertEquals(
                "can not convert SQS DTO to String",
                exception.getMessage()
        );

        assertSame(jsonException, exception.getCause());
    }

    @Test
    void enrichFluxContext_shouldAddAllValues() {
        Context context = Context.empty();

        Map<String, String> mdcContext = new HashMap<>();
        mdcContext.put(MDCUtils.MDC_TRACE_ID_KEY, "trace-id");
        mdcContext.put(MDCUtils.MDC_JTI_KEY, "jti");
        mdcContext.put(MDCUtils.MDC_PN_UID_KEY, "uid");
        mdcContext.put(MDCUtils.MDC_CX_ID_KEY, "cx-id");
        mdcContext.put(MDCUtils.MDC_PN_CX_TYPE_KEY, "cx-type");
        mdcContext.put(MDCUtils.MDC_PN_CX_GROUPS_KEY, "groups");
        mdcContext.put(MDCUtils.MDC_PN_CX_ROLE_KEY, "role");
        mdcContext.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, "message-id");
        mdcContext.put(CORRELATION_ID, "correlation-id");

        Context result =
                gatewayUtils.enrichFluxContext(context, mdcContext);

        assertEquals(
                "trace-id",
                result.get(MDCUtils.MDC_TRACE_ID_KEY)
        );
        assertEquals(
                "jti",
                result.get(MDCUtils.MDC_JTI_KEY)
        );
        assertEquals(
                "uid",
                result.get(MDCUtils.MDC_PN_UID_KEY)
        );
        assertEquals(
                "cx-id",
                result.get(MDCUtils.MDC_CX_ID_KEY)
        );
        assertEquals(
                "cx-type",
                result.get(MDCUtils.MDC_PN_CX_TYPE_KEY)
        );
        assertEquals(
                "groups",
                result.get(MDCUtils.MDC_PN_CX_GROUPS_KEY)
        );
        assertEquals(
                "role",
                result.get(MDCUtils.MDC_PN_CX_ROLE_KEY)
        );
        assertEquals(
                "message-id",
                result.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID)
        );
        assertEquals(
                "correlation-id",
                result.get(CORRELATION_ID)
        );
    }

    @Test
    void enrichFluxContext_shouldIgnoreNullValues() {
        Context context = Context.empty();

        Map<String, String> mdcContext = new HashMap<>();
        mdcContext.put(MDCUtils.MDC_TRACE_ID_KEY, "trace-id");
        mdcContext.put(MDCUtils.MDC_JTI_KEY, null);
        mdcContext.put(CORRELATION_ID, null);

        Context result =
                gatewayUtils.enrichFluxContext(context, mdcContext);

        assertEquals(
                "trace-id",
                result.get(MDCUtils.MDC_TRACE_ID_KEY)
        );

        assertFalse(result.hasKey(MDCUtils.MDC_JTI_KEY));
        assertFalse(result.hasKey(CORRELATION_ID));
    }

    @Test
    void enrichFluxContext_shouldReturnSameContextWhenMdcContextIsNull() {
        Context context =
                Context.of("existing-key", "existing-value");

        Context result =
                gatewayUtils.enrichFluxContext(context, null);

        assertSame(context, result);
        assertEquals(
                "existing-value",
                result.get("existing-key")
        );
    }

    @Test
    void enrichFluxContext_shouldPreserveExistingValues() {
        Context context =
                Context.of("existing-key", "existing-value");

        Map<String, String> mdcContext = new HashMap<>();
        mdcContext.put(CORRELATION_ID, "correlation-id");

        Context result =
                gatewayUtils.enrichFluxContext(context, mdcContext);

        assertEquals(
                "existing-value",
                result.get("existing-key")
        );
        assertEquals(
                "correlation-id",
                result.get(CORRELATION_ID)
        );
    }

    @Test
    void buildAndPrintRequestAuditLog_shouldReturnAuditLogEvent() {
        AddressQueryRequest request =
                AddressQueryRequest.builder()
                        .correlationId("correlation-id")
                        .recIndex(1)
                        .build();

        assertNotNull(
                gatewayUtils.buildAndPrintRequestAuditLog(
                        request,
                        GatewayDownstreamService.ANPR
                )
        );
    }

    @Test
    void handleException_shouldMapGenericException() {
        AddressQueryRequest request =
                AddressQueryRequest.builder()
                        .recIndex(10)
                        .build();

        RuntimeException exception =
                new RuntimeException("generic error");

        PhysicalAddressResponseDto result =
                gatewayUtils.handleException(
                        exception,
                        request,
                        GatewayDownstreamService.ANPR
                );

        assertNotNull(result);
        assertEquals(10, result.getRecIndex());
        assertEquals(
                GatewayDownstreamService.ANPR.name(),
                result.getRegistry()
        );
        assertEquals(
                GatewayError.DOWNSTREAM_REQUEST_ERROR.name(),
                result.getError()
        );
        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                result.getErrorStatus()
        );
    }

    @Test
    void handleException_shouldMapPnExceptionStatus() {
        AddressQueryRequest request =
                AddressQueryRequest.builder()
                        .recIndex(20)
                        .build();

        PnNationalRegistriesException exception =
                mock(PnNationalRegistriesException.class);

        when(exception.getStatusCode())
                .thenReturn(HttpStatus.BAD_GATEWAY);

        PhysicalAddressResponseDto result =
                gatewayUtils.handleException(
                        exception,
                        request,
                        GatewayDownstreamService.REGISTRO_IMPRESE
                );

        assertEquals(20, result.getRecIndex());

        assertEquals(
                GatewayDownstreamService.REGISTRO_IMPRESE.name(),
                result.getRegistry()
        );

        assertEquals(
                GatewayError.DOWNSTREAM_REQUEST_ERROR.name(),
                result.getError()
        );

        assertEquals(
                HttpStatus.BAD_GATEWAY.value(),
                result.getErrorStatus()
        );
    }

    @Test
    void handleException_shouldMapTooManyRequestsError() {
        AddressQueryRequest request =
                AddressQueryRequest.builder()
                        .recIndex(30)
                        .build();

        PnNationalRegistriesException exception =
                mock(PnNationalRegistriesException.class);

        when(exception.getStatusCode())
                .thenReturn(HttpStatus.TOO_MANY_REQUESTS);

        PhysicalAddressResponseDto result =
                gatewayUtils.handleException(
                        exception,
                        request,
                        GatewayDownstreamService.ANPR
                );

        assertEquals(
                GatewayError.DOWNSTREAM_TOO_MANY_REQUESTS.name(),
                result.getError()
        );

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                result.getErrorStatus()
        );
    }

    @Test
    void handleException_shouldUseCorrectRegistry() {
        AddressQueryRequest request =
                AddressQueryRequest.builder()
                        .recIndex(5)
                        .build();

        PhysicalAddressResponseDto result =
                gatewayUtils.handleException(
                        new RuntimeException(),
                        request,
                        GatewayDownstreamService.REGISTRO_IMPRESE
                );

        assertEquals(
                GatewayDownstreamService.REGISTRO_IMPRESE.name(),
                result.getRegistry()
        );
    }
}
