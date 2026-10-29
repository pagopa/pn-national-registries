package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.agenziaentrate.ResultCodeEnum;
import it.pagopa.pn.national.registries.model.agenziaentrate.ResultDetailEnum;
import it.pagopa.pn.national.registries.service.AgenziaEntrateService;
import it.pagopa.pn.national.registries.utils.RequestIdUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgenziaEntrateControllerTest {
    private AgenziaEntrateController agenziaEntrateController;
    private ServerWebExchange serverWebExchange;
    private HttpHeaders httpHeaders;
    private static final String AWS_XRAY_TRACE_ID = "AWS-XRAY-TRACE-ID";

    @Mock
    AgenziaEntrateService agenziaEntrateService;

    @BeforeEach
    void setUp() {
        serverWebExchange = mock(ServerWebExchange.class);
        ServerHttpResponse serverHttpResponse = mock(ServerHttpResponse.class);
        httpHeaders = new HttpHeaders();

        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.getHeaders()).thenReturn(httpHeaders);
        agenziaEntrateController = new AgenziaEntrateController(agenziaEntrateService);
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void checkTaxId() {

        CheckTaxIdRequestBodyDto checkTaxIdRequestBodyDto = new CheckTaxIdRequestBodyDto();
        CheckTaxIdRequestBodyFilterDto dto = new CheckTaxIdRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        checkTaxIdRequestBodyDto.setFilter(dto);

        CheckTaxIdOKDto checkTaxIdOKDto = new CheckTaxIdOKDto();
        checkTaxIdOKDto.setTaxId("PPPPLT80A01H501V");
        checkTaxIdOKDto.setIsValid(true);

        when(agenziaEntrateService.callEService(checkTaxIdRequestBodyDto))
                .thenReturn((Mono.fromSupplier(() -> checkTaxIdOKDto)));

        StepVerifier.create(agenziaEntrateController.checkTaxId(Mono.just(checkTaxIdRequestBodyDto), serverWebExchange))
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(checkTaxIdOKDto, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    void checkTaxIdAndVatNumber() {

        ADELegalRequestBodyDto adeLegalRequestBodyDto = new ADELegalRequestBodyDto();
        ADELegalRequestBodyFilterDto adeLegalRequestBodyFilterDto = new ADELegalRequestBodyFilterDto();
        adeLegalRequestBodyFilterDto.setTaxId("PPPPLT80A01H501V");
        adeLegalRequestBodyFilterDto.setVatNumber("testVatNumber");
        adeLegalRequestBodyDto.setFilter(adeLegalRequestBodyFilterDto);
        ADELegalOKDto adeLegalOKDto = new ADELegalOKDto();
        adeLegalOKDto.setResultCode(ResultCodeEnum.fromValue("00"));
        adeLegalOKDto.setVerificationResult(true);
        adeLegalOKDto.setResultDetail(ResultDetailEnum.getCode("XX00"));

        when(agenziaEntrateService.checkTaxIdAndVatNumber(adeLegalRequestBodyDto))
                .thenReturn((Mono.fromSupplier(() -> adeLegalOKDto)));

        StepVerifier.create(agenziaEntrateController.adeLegal(Mono.just(adeLegalRequestBodyDto), serverWebExchange))
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(adeLegalOKDto, response.getBody());
                })
                .verifyComplete();
    }
}
