package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.TaxIdFilterDto;
import it.pagopa.pn.national.registries.service.IpaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class IpaControllerTest {
    private ServerWebExchange serverWebExchange;
    private HttpHeaders httpHeaders;

    @InjectMocks
    private IpaController ipaController;

    @Mock
    private IpaService ipaService;

    @BeforeEach
    void init() {
        serverWebExchange = mock(ServerWebExchange.class);
        ServerHttpResponse serverHttpResponse = mock(ServerHttpResponse.class);
        httpHeaders = new HttpHeaders();

        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.getHeaders()).thenReturn(httpHeaders);
        ipaController = new IpaController(ipaService);
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void testIpaPec4() {
        IPAPecDto ipaPecDto = new IPAPecDto();
        when(ipaService.getIpaPec(any())).thenReturn(Mono.just(ipaPecDto));

        TaxIdFilterDto checkTaxIdRequestBodyFilterDto = new TaxIdFilterDto();
        checkTaxIdRequestBodyFilterDto.taxId("42");
        checkTaxIdRequestBodyFilterDto.setCorrelationId("correlation-id");

        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        ipaRequestBodyDto.filter(checkTaxIdRequestBodyFilterDto);
        ipaController.ipaPec(Mono.just(ipaRequestBodyDto), serverWebExchange);
        when(ipaService.getIpaPec(ipaRequestBodyDto))
                .thenReturn(Mono.fromSupplier(() -> {
                    assertEquals("correlation-id", MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
                    return ipaPecDto;
                }));

        StepVerifier.create(ipaController.ipaPec(Mono.just(ipaRequestBodyDto), serverWebExchange))
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(ipaPecDto, response.getBody());
                    assertEquals("correlation-id", httpHeaders.getFirst("X-Request-ID"));
                })
                .verifyComplete();
    }


}

