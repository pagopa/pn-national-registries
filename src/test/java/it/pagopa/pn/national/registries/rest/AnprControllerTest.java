package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyFilterDto;
import it.pagopa.pn.national.registries.service.AnprService;
import it.pagopa.pn.national.registries.service.GatewayService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnprControllerTest {
    private ServerWebExchange serverWebExchange;
    private HttpHeaders httpHeaders;

    AnprController anprController;

    @Mock
    AnprService anprService;


    @BeforeEach
    void init() {
        serverWebExchange = mock(ServerWebExchange.class);
        ServerHttpResponse serverHttpResponse = mock(ServerHttpResponse.class);
        httpHeaders = new HttpHeaders();

        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.getHeaders()).thenReturn(httpHeaders);
        anprController = new AnprController(anprService, new GatewayService(null, null, null, null, null));
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void testGetAddressANPR() {
        String traceId = "traceId";
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
        GetAddressANPRRequestBodyDto getAddressANPRRequestBodyDto = new GetAddressANPRRequestBodyDto();
        GetAddressANPRRequestBodyFilterDto dto = new GetAddressANPRRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        getAddressANPRRequestBodyDto.setFilter(dto);

        GetAddressANPROKDto getAddressANPROKDto = new GetAddressANPROKDto();
        getAddressANPROKDto.setResidentialAddresses(new ArrayList<>());

        when(anprService.getAddressANPR(getAddressANPRRequestBodyDto))
                .thenReturn(Mono.fromSupplier(() -> {
                    assertEquals(traceId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
                    return getAddressANPROKDto;
                }));

        StepVerifier.create(anprController.addressANPR(Mono.just(getAddressANPRRequestBodyDto), serverWebExchange))
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(getAddressANPROKDto, response.getBody());
                    assertEquals(traceId, httpHeaders.getFirst("X-Request-ID"));
                })
                .verifyComplete();
    }
}
