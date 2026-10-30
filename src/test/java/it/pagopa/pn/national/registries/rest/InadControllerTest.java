package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADRequestBodyFilterDto;
import it.pagopa.pn.national.registries.service.InadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class InadControllerTest {
    private ServerWebExchange serverWebExchange;
    private HttpHeaders httpHeaders;

    @InjectMocks
    InadController inadController;

    @Mock
    InadService inadService;

    @BeforeEach
    void init() {
        serverWebExchange = mock(ServerWebExchange.class);
        ServerHttpResponse serverHttpResponse = mock(ServerHttpResponse.class);
        httpHeaders = new HttpHeaders();

        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.getHeaders()).thenReturn(httpHeaders);
        inadController = new InadController(inadService);
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void getDigitalAddressINAD() {
        GetDigitalAddressINADRequestBodyDto extractDigitalAddressINADRequestBodyDto = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto dto = new GetDigitalAddressINADRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        dto.setCorrelationId("correlation-id");
        extractDigitalAddressINADRequestBodyDto.setFilter(dto);

        GetDigitalAddressINADOKDto getDigitalAddressINADOKDto = new GetDigitalAddressINADOKDto();
        getDigitalAddressINADOKDto.setTaxId("DDDFGF52F52H501S");
        getDigitalAddressINADOKDto.setSince(new Date());

        when(inadService.callEService(extractDigitalAddressINADRequestBodyDto, RecipientType.PF, "correlation-id"))
                .thenReturn(Mono.fromSupplier(() -> {
                    assertEquals("correlation-id", MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
                    return getDigitalAddressINADOKDto;
                }));

        StepVerifier.create(inadController.digitalAddressINAD("PF", Mono.just(extractDigitalAddressINADRequestBodyDto), serverWebExchange))
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(getDigitalAddressINADOKDto, response.getBody());
                    assertEquals("correlation-id", httpHeaders.getFirst("X-Request-ID"));
                })
                .verifyComplete();
    }
}
